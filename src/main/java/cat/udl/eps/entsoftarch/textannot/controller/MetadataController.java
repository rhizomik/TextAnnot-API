package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.exception.NotFoundException;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import javassist.tools.web.BadHttpRequest;
import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@BasePathAwareController
public class MetadataController {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MetadataFieldRepository metadataFieldRepository;

    @Autowired
    ProjectRepository projectRepository;

    JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(entityManager);
    }

    @GetMapping("/metadataFields/{id}/value-counts")
    public @ResponseBody
    MetadataFieldValueCounts
    getMetadataFieldValuesCount(@PathVariable("id") Integer id) {
        MetadataField metadataField = metadataFieldRepository.findById(id).orElse(null);
        if (metadataField == null)
            throw new NotFoundException();
        List<Tuple> result = queryFactory.select(QMetadataValue.metadataValue.value, QMetadataValue.metadataValue.value.count())
                .from(QMetadataValue.metadataValue).innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                .where(QMetadataField.metadataField.id.eq(id))
                .groupBy(QMetadataValue.metadataValue.value).fetch();
        return new MetadataFieldValueCounts(
                metadataField.getName(),
                result.stream().collect(Collectors.toMap(t -> t.get(0, String.class),
                        t -> t.get(1, Long.class)))
        );
    }

    @PostMapping("/metadataFields/{id}/values-edit")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void renameMetadataFieldValues(
            @PathVariable("id") Integer id,
            @RequestBody MetadataFieldValuesRename valuesRename) {
        valuesRename.getRenames().forEach(renaming -> renameFieldValue(id, renaming.get(0), renaming.get(1)));
    }

    public void renameFieldValue(Integer metadataFieldId, String oldValue, String newValue) {
        queryFactory.update(QMetadataValue.metadataValue)
                .set(QMetadataValue.metadataValue.value, newValue)
                .where(QMetadataValue.metadataValue.values.id.eq(metadataFieldId)
                        .and(QMetadataValue.metadataValue.value.eq(oldValue)))
                .execute();
    }

    @GetMapping("/metadataValues/search/findDistinctByField")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody MetadataDistinctValues getDistinctMetadataValueByField(@RequestParam("name") String name) {
        MetadataDistinctValues values = new MetadataDistinctValues();
        values.setValues(
                queryFactory.selectDistinct(QMetadataValue.metadataValue.value)
                .from(QMetadataValue.metadataValue)
                        .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                        .where(QMetadataField.metadataField.name.eq(name)).fetch());
        return values;
    }

    @PostMapping("/project/{projectId}/metadata/use-default")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public void useDefaultMetadata(
            @PathVariable("projectId") Integer projectId) throws IOException {
        Optional<Project> project = projectRepository.findById(projectId);
        if (!project.isPresent())
            throw new NotFoundException();

        readMetadataCsv(getClass().getClassLoader().getResourceAsStream("metadata.csv"), project.get());

    }

    private void readMetadataCsv(InputStream metadataCsv, Project project) throws IOException {
        CSVFormat excelCSV = CSVFormat.newFormat(';').withRecordSeparator('\n').withFirstRecordAsHeader();
        CSVParser parser = CSVParser.parse(metadataCsv, StandardCharsets.UTF_8, excelCSV);
        parser.getRecords().forEach(record -> {
            MetadataField metadataField = new MetadataField();
            metadataField.setDefinedAt(project);
            metadataField.setCategory(record.get(0));
            metadataField.setName(record.get(1));
            metadataField.setXmlName(record.get(2));
            metadataField.setType(MetadataField.FieldType.valueOf(record.get(3).toUpperCase()));
            metadataField.setPrivateField(Boolean.valueOf(record.get(4)));
            metadataField.setIncludeStatistics(Boolean.valueOf(record.get(5)));
            metadataFieldRepository.save(metadataField);
        });
    }


    @Data
    private static class MetadataFieldValueCounts {
        String fieldName;
        Map<String, Long> valueCounts;

        public MetadataFieldValueCounts(String fieldName, Map<String, Long> valueCounts) {
            this.fieldName = fieldName;
            this.valueCounts = valueCounts;
        }
    }

    @Data
    private static class MetadataFieldValuesRename {
        List<List<String>> renames;
    }

    @Data
    private static class MetadataDistinctValues {
        List<String> values;
    }
}
