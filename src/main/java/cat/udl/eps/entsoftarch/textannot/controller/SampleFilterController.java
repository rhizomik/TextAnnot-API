package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@BasePathAwareController
public class SampleFilterController {

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    EntityManager em;

    @PostMapping("/samples/filter")
    public @ResponseBody
    PagedResources<PersistentEntityResource> getFilteredSamples(@RequestBody SampleFilters filters, Pageable pageable, PagedResourcesAssembler resourceAssembler) {
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
        if (filters.getMetadata() != null && !filters.getMetadata().isEmpty()) {
            for (Map.Entry<String, String> e : filters.getMetadata().entrySet()) {
                query = query.and(QSample.sample.id.in(JPAExpressions.select(QMetadataValue.metadataValue.forA.id).from(QMetadataValue.metadataValue)
                        .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                        .where(QMetadataValue.metadataValue.value.eq(e.getValue()).and(QMetadataField.metadataField.name.eq(e.getKey())))));
            }
        }
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
            for (String tag : filters.getTags()) {
                query = query.and(JPAExpressions.selectFrom(QAnnotation.annotation).innerJoin(QAnnotation.annotation.tag, QTag.tag)
                        .where(QAnnotation.annotation.sample.id.eq(QSample.sample.id).and(QTag.tag.name.eq(tag))).exists());
            }
        }
        return query;
    }

    @PostMapping("/samples/filter/statistics")
    public @ResponseBody
    StatisticsResults getFilteredSamplesStatistics(@RequestBody SampleFilters filters) {
        StatisticsResults statisticsResults = new StatisticsResults();
        statisticsResults.setMetadataStadistics(getMetadataStatistics(filters));
        Pair<Long, Long> counts = getSampleCounts(filters);
        statisticsResults.setOccurrences(counts.getFirst());
        statisticsResults.setSamples(counts.getSecond());
        return statisticsResults;
    }

    private Pair<Long, Long> getSampleCounts(SampleFilters filters) {
        final AtomicLong occurrences = new AtomicLong(0L);
        final AtomicLong samplesCount = new AtomicLong(0L);
        Iterable<Sample> samples = sampleRepository.findAll(getFiltersExpression(filters));
        samples.forEach(sample -> {
            samplesCount.incrementAndGet();
            occurrences.addAndGet(getTextOccurrences(filters.getWord(), sample.getText()));
        });
        return Pair.of(new Long(occurrences.get()), new Long(samplesCount.get()));
    }

    private int getTextOccurrences(String word, String text) {
        Pattern pattern = Pattern.compile("\\b" + word + "\\b");
        Matcher matcher = pattern.matcher(text);
        int sampleOccurrences = 0;
        while (matcher.find())
            sampleOccurrences++;
        return sampleOccurrences;
    }

    private Map<String, Map<String, Long>> getMetadataStatistics(@RequestBody SampleFilters filters) {
        JPAQuery query = new JPAQuery(em);
        NumberPath<Long> aliasCount = Expressions.numberPath(Long.class, "c");
        query = (JPAQuery) query.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QSample.sample.count().as(aliasCount))
                .from(QMetadataValue.metadataValue)
                .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                .where(getFiltersExpression(filters))
                .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value)
                .orderBy(aliasCount.desc());
        List<Tuple> result = query.fetch();
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        result.forEach(qTuple -> {
            if (!statistics.containsKey(qTuple.get(QMetadataField.metadataField.name)))
                statistics.put(qTuple.get(QMetadataField.metadataField.name), new LinkedHashMap<>());
            statistics.get(qTuple.get(QMetadataField.metadataField.name)).put(qTuple.get(QMetadataValue.metadataValue.value), qTuple.get(2, Long.TYPE));
        });
        return statistics;
    }

    private static class StatisticsResults {
        private long occurrences;
        private long samples;
        private Map<String, Map<String, Long>> metadataStadistics;

        public long getOccurrences() {
            return occurrences;
        }

        public void setOccurrences(long occurrences) {
            this.occurrences = occurrences;
        }

        public long getSamples() {
            return samples;
        }

        public void setSamples(long samples) {
            this.samples = samples;
        }

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
