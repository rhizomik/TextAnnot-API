package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@BasePathAwareController
public class SampleFilterController {

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(em);
    }

    @GetMapping("/samples/filter")
    public @ResponseBody
    PagedResources<PersistentEntityResource> getFilteredSamples(@RequestParam("word") String word,
                                                                @RequestParam("tags") List<String> tags,
                                                                @RequestParam Map<String, String> params,
                                                                Pageable pageable, PagedResourcesAssembler resourceAssembler) {

        BooleanBuilder query = getFiltersExpression(new SampleFilters(word, getMetadataMap(params), tags));
        Page<Sample> samples = sampleRepository.findAll(query, pageable);

        if (!samples.hasContent()) {
            return resourceAssembler.toEmptyResource(samples, Sample.class);
        }

        return resourceAssembler.toResource(samples);
    }

    @GetMapping("/samples/filter/statistics")
    public @ResponseBody
    StatisticsResults getFilteredSamplesStatistics(@RequestParam("word") String word,
                                                   @RequestParam("tags") List<String> tags,
                                                   @RequestParam Map<String, String> params) {
        SampleFilters filters = new SampleFilters(word, getMetadataMap(params), tags);
        StatisticsResults statisticsResults = new StatisticsResults();
        statisticsResults.setMetadataStatistics(getMetadataStatistics(filters));
        statisticsResults.setGlobalMetadataStatistics(getMetadataStatistics(new SampleFilters()));
        Pair<Long, Long> counts = getSampleCounts(filters);
        statisticsResults.setOccurrences(counts.getFirst());
        statisticsResults.setSamples(counts.getSecond());
        Pair<Long, Long> globalCounts = getSampleCounts(new SampleFilters());
        statisticsResults.setTotalSamples(globalCounts.getSecond());
        statisticsResults.setAnnotationStatistics(getAnnotationStatistics(filters));
        return statisticsResults;
    }

    private Set<AnnotationStatistics> getAnnotationStatistics(SampleFilters filters) {
        List<Tuple> result = queryFactory.select(QTag.tag.name, QTag.tag.treePath, QSample.sample.count(), QSample.sample.countDistinct())
                .from(QAnnotation.annotation)
                .innerJoin(QAnnotation.annotation.sample, QSample.sample)
                .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                .where(getFiltersExpression(filters))
                .groupBy(QTag.tag.name).fetch();
        List<Tuple> globalResult = queryFactory.select(QTag.tag.name, QSample.sample.countDistinct())
                .from(QAnnotation.annotation)
                .innerJoin(QAnnotation.annotation.sample, QSample.sample)
                .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                .groupBy(QTag.tag.name).fetch();
        Map<String, Long> globalResultMap = globalResult.stream().collect(Collectors.toMap((Tuple t) -> t.get(QTag.tag.name), (Tuple t) -> t.get(1, Long.TYPE)));
        Map<String, AnnotationStatistics> annotationStatisticsMap = new HashMap<>();

         Set<AnnotationStatistics> annotationStatisticsList = result.stream().map(tuple -> updateStatisticsTreeAndGetRoot(annotationStatisticsMap, tuple.get(QTag.tag.treePath),
                new AnnotationStatistics(tuple.get(QTag.tag.name), tuple.get(2, Long.class),
                        tuple.get(3, Long.class), globalResultMap.get(tuple.get(QTag.tag.name)))))
                 .collect(Collectors.toSet());
         annotationStatisticsList.stream().forEach(annotationStatistics -> annotationStatistics.calculateStatistics());
        return annotationStatisticsList;
    }

    private AnnotationStatistics updateStatisticsTreeAndGetRoot(Map<String, AnnotationStatistics> annotationStatisticsMap,
                                                                String tagPath, AnnotationStatistics tagStatistics) {
        AnnotationStatistics child = tagStatistics;
        String[] tags = tagPath.split(";");
        child.setLevel(tags.length - 1);
        annotationStatisticsMap.put(tagStatistics.tag, tagStatistics);
        for (int i = tags.length - 2; i >= 0; i--) {
            if (annotationStatisticsMap.containsKey(tags[i])) {
                annotationStatisticsMap.get(tags[i]).addChildStatistics(child);
                return annotationStatisticsMap.get(tags[0]);
            } else {
                child = new AnnotationStatistics(tags[i], i, child);
                annotationStatisticsMap.put(tags[i], child);
            }
        }
        return child;
    }

    private Map<String, String> getMetadataMap(Map<String, String> params) {
        params.remove("word");
        params.remove("tags");
        params.remove("page");
        params.remove("size");
        return params;
    }

    private BooleanBuilder getFiltersExpression(SampleFilters filters) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (filters.getWord() != null && !filters.getWord().isEmpty()) {
            List<Integer> samplesContainingWord = sampleRepository.findByTextContainingWord(filters.getWord());
            booleanBuilder = booleanBuilder.and(QSample.sample.id.in(samplesContainingWord));
        }
        if (filters.getMetadata() != null && !filters.getMetadata().isEmpty()) {
            for (Map.Entry<String, String> e : filters.getMetadata().entrySet()) {
                booleanBuilder = booleanBuilder.and(QSample.sample.id.in(JPAExpressions.select(QMetadataValue.metadataValue.forA.id).from(QMetadataValue.metadataValue)
                        .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                        .where(QMetadataValue.metadataValue.value.eq(e.getValue()).and(QMetadataField.metadataField.name.eq(e.getKey())))));
            }
        }
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
            for (String tag : filters.getTags()) {
                booleanBuilder = booleanBuilder.and(JPAExpressions.selectFrom(QAnnotation.annotation).innerJoin(QAnnotation.annotation.tag, QTag.tag)
                        .where(QAnnotation.annotation.sample.id.eq(QSample.sample.id).and(QTag.tag.name.eq(tag))).exists());
            }
        }
        return booleanBuilder;
    }

    private Pair<Long, Long> getSampleCounts(SampleFilters filters) {
        final AtomicLong occurrences = new AtomicLong(0L);
        final AtomicLong samplesCount = new AtomicLong(0L);
        Iterable<Sample> samples = sampleRepository.findAll(getFiltersExpression(filters));
        if (filters.getWord() != null && !filters.getWord().isEmpty())
            samples.forEach(sample -> {
                samplesCount.incrementAndGet();
                occurrences.addAndGet(getTextOccurrences(filters.getWord(), sample.getText()));
            });
        else
            samplesCount.set(StreamSupport.stream(samples.spliterator(), false).count());
        return Pair.of(new Long(occurrences.get()), new Long(samplesCount.get()));
    }

    private int getTextOccurrences(String word, String text) {
        Pattern pattern = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int sampleOccurrences = 0;
        while (matcher.find())
            sampleOccurrences++;
        return sampleOccurrences;
    }

    private Map<String, Map<String, Long>> getMetadataStatistics(SampleFilters filters) {
        List<Tuple> result = queryFactory.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QSample.sample.count())
                .from(QMetadataValue.metadataValue)
                .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                .where(getFiltersExpression(filters).and(QMetadataField.metadataField.includeStatistics.eq(true)))
                .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value).fetch();
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        result.forEach(qTuple -> {
            if (!statistics.containsKey(qTuple.get(QMetadataField.metadataField.name)))
                statistics.put(qTuple.get(QMetadataField.metadataField.name), new LinkedHashMap<>());
            statistics.get(qTuple.get(QMetadataField.metadataField.name)).put(qTuple.get(QMetadataValue.metadataValue.value), qTuple.get(2, Long.TYPE));
        });
        return statistics;
    }

    @Data
    private static class StatisticsResults {
        private long occurrences;
        private long samples;
        private long totalSamples;
        private Map<String, Map<String, Long>> metadataStatistics;
        private Map<String, Map<String, Long>> globalMetadataStatistics;
        private Set<AnnotationStatistics> annotationStatistics;
    }

    @Data
    private static class AnnotationStatistics {
        private String tag;
        private long occurrences;
        private long samples;
        private long globalSamples;
        private int level;
        private List<AnnotationStatistics> childrenStatistics;

        public AnnotationStatistics(String tag, long occurrences, long samples, long globalSamples) {
            this.tag = tag;
            this.level = 0;
            this.occurrences = occurrences;
            this.samples = samples;
            this.globalSamples = globalSamples;
            this.childrenStatistics = new ArrayList<>();
        }

        public AnnotationStatistics(String tag, Integer level, AnnotationStatistics child) {
            this(tag, 0, 0, 0);
            this.level = level;
            this.childrenStatistics.add(child);

        }

        public void addChildStatistics(AnnotationStatistics child) {
            this.childrenStatistics.add(child);
        }

        public AnnotationStatistics calculateStatistics() {
            childrenStatistics.forEach(annotationStatistics -> this.updateStatistics(annotationStatistics.calculateStatistics()));
            return this;
        }

        private void updateStatistics(AnnotationStatistics annotationStatistics) {
            this.occurrences += annotationStatistics.getOccurrences();
            this.samples += annotationStatistics.getSamples();
            this.globalSamples += annotationStatistics.getGlobalSamples();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnnotationStatistics that = (AnnotationStatistics) o;
            return Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag);
        }
    }

    @Data
    private static class SampleFilters implements Serializable {
        private String word;
        private Map<String, String> metadata;
        private List<String> tags;

        public SampleFilters() {
        }

        public SampleFilters(String word, Map<String, String> metadata, List<String> tags) {
            this.word = word;
            this.metadata = metadata;
            this.tags = tags;
        }
    }

}
