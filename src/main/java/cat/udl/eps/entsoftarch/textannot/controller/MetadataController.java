package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.MetadataValue;
import cat.udl.eps.entsoftarch.textannot.domain.QMetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.QMetadataValue;
import cat.udl.eps.entsoftarch.textannot.exception.NotFoundException;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @GetMapping("/metadataFields/{id}/value-counts")
    public @ResponseBody
    MetadataFieldValueCounts
    getMetadataFieldValuesCount(@PathVariable("id") Integer id) {
        MetadataField metadataField = metadataFieldRepository.findById(id).orElse(null);
        if (metadataField == null)
            throw new NotFoundException();
        JPAQuery query = new JPAQuery(entityManager);
        query = (JPAQuery) query.select(QMetadataValue.metadataValue.value, QMetadataValue.metadataValue.value.count())
                .from(QMetadataValue.metadataValue).innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                .where(QMetadataField.metadataField.id.eq(id))
                .groupBy(QMetadataValue.metadataValue.value);
        List<Tuple> result = query.fetch();
        return new MetadataFieldValueCounts(
                metadataField.getName(),
                result.stream().collect(Collectors.toMap(t -> t.get(0, String.class),
                        t -> t.get(1, Long.class)))

        );

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
}
