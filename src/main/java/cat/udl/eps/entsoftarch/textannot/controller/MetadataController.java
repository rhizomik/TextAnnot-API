package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.MetadataValue;
import cat.udl.eps.entsoftarch.textannot.domain.QMetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.QMetadataValue;
import cat.udl.eps.entsoftarch.textannot.exception.NotFoundException;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@BasePathAwareController
public class MetadataController {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MetadataFieldRepository metadataFieldRepository;

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
