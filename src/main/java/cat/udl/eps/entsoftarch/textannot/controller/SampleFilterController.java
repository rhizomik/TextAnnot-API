package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.QTuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BasePathAwareController
public class SampleFilterController {

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    EntityManager em;

    @PostMapping("/samples/filter")
    public @ResponseBody PagedResources<PersistentEntityResource> getFilteredSamples(@RequestBody SampleFilters filters, Pageable pageable, PagedResourcesAssembler resourceAssembler) {
        BooleanExpression query = getFiltersExpression(filters);
        Page<Sample> samples = sampleRepository.findAll(query, pageable);

        if (!samples.hasContent()) {
            return resourceAssembler.toEmptyResource(samples, Sample.class);
        }

        return resourceAssembler.toResource(samples);
    }

    private BooleanExpression getFiltersExpression(@RequestBody SampleFilters filters) {
        List<Integer> samplesContainingWord = sampleRepository.findByTextContainingWord(filters.getWord());
        BooleanExpression query = QSample.sample.id.in(samplesContainingWord);
        if(filters.getMetadata() != null && !filters.getMetadata().isEmpty()) {
            for (Map.Entry<String, String> e: filters.getMetadata().entrySet()) {
                 query = query.and(QSample.sample.id.in(JPAExpressions.select(QMetadataValue.metadataValue.forA.id).from(QMetadataValue.metadataValue)
                         .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                         .where(QMetadataValue.metadataValue.value.eq(e.getValue()).and(QMetadataField.metadataField.name.eq(e.getKey())))));
            }
        }
        if(filters.getTags() != null && !filters.getTags().isEmpty()) {
            for (String tag: filters.getTags()) {
                query = query.and(JPAExpressions.selectFrom(QAnnotation.annotation).innerJoin(QAnnotation.annotation.tag, QTag.tag)
                        .where(QAnnotation.annotation.sample.id.eq(QSample.sample.id).and(QTag.tag.name.eq(tag))).exists());
            }
        }
        return query;
    }

    @PostMapping("/samples/filter/statistics")
    public @ResponseBody StatisticsResults getFilteredSamplesStatistics(@RequestBody SampleFilters filters) {
        StatisticsResults statisticsResults = new StatisticsResults();
        statisticsResults.setMetadataStadistics(getMetadataStatistics(filters));

        return statisticsResults;
    }

    private Map<String, Map<String, Long>> getMetadataStatistics(@RequestBody SampleFilters filters) {
        JPAQuery query = new JPAQuery(em);
        query = (JPAQuery) query.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QSample.sample.count())
                .from(QMetadataValue.metadataValue).innerJoin(QMetadataValue.metadataValue.forA, QSample.sample).innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                .where(getFiltersExpression(filters)).groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value);
        List<Tuple> result = query.fetch();
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        result.forEach(qTuple -> {
            if (!statistics.containsKey(qTuple.get(QMetadataField.metadataField.name)))
                statistics.put(qTuple.get(QMetadataField.metadataField.name), new HashMap<>());
            statistics.get(qTuple.get(QMetadataField.metadataField.name)).put(qTuple.get(QMetadataValue.metadataValue.value), qTuple.get(2, Long.TYPE));
        });
        return statistics;
    }

    private static class StatisticsResults implements Serializable {
        private Map<String, Map<String, Long>> metadataStadistics;

        public Map<String, Map<String, Long>> getMetadataStadistics() {
            return metadataStadistics;
        }

        public void setMetadataStadistics(Map<String, Map<String, Long>> metadataStadistics) {
            this.metadataStadistics = metadataStadistics;
        }
    }

    private static class SampleFilters implements Serializable {
        private String word;
        private Map<String, String> metadata;
        private List<String> tags;

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

}
