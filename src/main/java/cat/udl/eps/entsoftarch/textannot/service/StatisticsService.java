package cat.udl.eps.entsoftarch.textannot.service;

import cat.udl.eps.entsoftarch.textannot.controller.SampleFilterController;
import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class StatisticsService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private MetadataFieldRepository metadataFieldRepository;

    @Autowired
    private ControllerUtilities controllerUtilities;

    JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(entityManager);
    }

    public Map<String, Map<String, Long>> getMetadataStatistics(Project project, SampleFilterController.SampleFilters filters) {
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        statistics.putAll(getStringMetadataStatistics(project, filters));
        statistics.putAll(getIntegerMetadataStatistics(project, filters));
        return statistics;
    }

    public Map<String, Map<String, Long>> getProjectMetadataStatistics(Project project) {
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        List<Tuple> result;
        result = queryFactory.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QSample.sample.id.count())
                .from(QMetadataValue.metadataValue)
                .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                .where(QMetadataField.metadataField.includeStatistics.eq(true)
                        .and(QMetadataField.metadataField.type.eq(MetadataField.FieldType.STRING)))
                .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value).fetch();

        result.forEach(qTuple -> {
            if (!statistics.containsKey(qTuple.get(QMetadataField.metadataField.name)))
                statistics.put(qTuple.get(QMetadataField.metadataField.name), new LinkedHashMap<>());
            statistics.get(qTuple.get(QMetadataField.metadataField.name)).put(qTuple.get(QMetadataValue.metadataValue.value), qTuple.get(2, Long.TYPE));
        });
        return statistics;
    }

    public Map<String, Map<String, Long>> getGlobalMetadataStatistics(Project project, SampleFilterController.SampleFilters filters) {
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        statistics.putAll(getGlobalStringMetadataStatistics(project, filters));
        statistics.putAll(getIntegerMetadataStatistics(project, new SampleFilterController.SampleFilters()));
        return statistics;
    }

    public Pair<Long, Long> getSampleCounts(Project project, SampleFilterController.SampleFilters filters) {
        final AtomicLong occurrences = new AtomicLong(0L);
        final AtomicLong samplesCount = new AtomicLong(0L);
        Iterable<Sample> samples = sampleRepository.findAll(getFiltersExpression(project, filters));
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
            List<Sample> samplesList = new ArrayList<>();
            samples.forEach(samplesList::add);
            List<Annotation> result = queryFactory.selectFrom(QAnnotation.annotation)
                    .innerJoin(QAnnotation.annotation.sample, QSample.sample)
                    .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(QSample.sample.in(samplesList).and(controllerUtilities.getTagQuery(filters.getTags())))
                    .groupBy(QSample.sample.id, QAnnotation.annotation.start, QAnnotation.annotation.end)
                    .having(QTag.tag.name.countDistinct().goe((long) filters.getTags().size())).fetch();
            occurrences.set(result.size());
            samplesCount.set(result.stream().map(Annotation::getSample).distinct().count());
        } else if (filters.getWord() != null && !filters.getWord().isEmpty())
            samples.forEach(sample -> {
                samplesCount.incrementAndGet();
                occurrences.addAndGet(getTextOccurrences(filters.getWord(), sample.getText()));
            });
        else
            samplesCount.set(StreamSupport.stream(samples.spliterator(), false).count());
        return Pair.of(new Long(occurrences.get()), new Long(samplesCount.get()));
    }

    public Pair<Long, Long> getGlobalSampleCounts(Project project, SampleFilterController.SampleFilters filters) {
        final AtomicLong occurrences = new AtomicLong(0L);
        final AtomicLong samplesCount = new AtomicLong(0L);
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
            List<Annotation> result = queryFactory.selectFrom(QAnnotation.annotation)
                    .innerJoin(QAnnotation.annotation.sample, QSample.sample)
                    .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(controllerUtilities.getTagQuery(filters.getTags()))
                    .groupBy(QSample.sample.id, QAnnotation.annotation.start, QAnnotation.annotation.end)
                    .having(QTag.tag.name.countDistinct().eq((long) filters.getTags().size())).fetch();
            occurrences.set(result.size());
            samplesCount.set(result.stream().map(Annotation::getSample).distinct().count());
        } else {
            Iterable<Sample> samples = sampleRepository.findAll(getFiltersExpression(project, filters));
            if (filters.getWord() != null && !filters.getWord().isEmpty()) {
                samples.forEach(sample -> {
                    samplesCount.incrementAndGet();
                    occurrences.addAndGet(getTextOccurrences(filters.getWord(), sample.getText()));
                });
            } else
                samplesCount.set(StreamSupport.stream(samples.spliterator(), false).count());
        }
        return Pair.of(new Long(occurrences.get()), new Long(samplesCount.get()));
    }

    public BooleanBuilder getFiltersExpression(Project project, SampleFilterController.SampleFilters filters) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(QSample.sample.id.in(
                JPAExpressions.select(QSample.sample.id).from(QSample.sample).innerJoin(QSample.sample.project, QProject.project).where(QProject.project.eq(project))));
        if (filters.getWord() != null && !filters.getWord().isEmpty()) {
            List<Integer> samplesContainingWord = sampleRepository.findByTextContainingWord(filters.getWord().replace("*", "[A-Za-zÀ-ÖØ-öø-ÿ]*"));
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
            booleanBuilder = booleanBuilder.and(JPAExpressions.selectFrom(QAnnotation.annotation).innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(QAnnotation.annotation.sample.id.eq(QSample.sample.id).and(controllerUtilities.getTagQuery(filters.getTags())))
                    .groupBy(QAnnotation.annotation.sample.id, QAnnotation.annotation.start, QAnnotation.annotation.end).having(QTag.tag.name.countDistinct().goe(((long) filters.getTags().size()))).exists());
        }
        return booleanBuilder;
    }

    private Map<? extends String,? extends Map<String, Long>> getIntegerMetadataStatistics(Project project, SampleFilterController.SampleFilters filters) {
        List<MetadataField> integerFields = metadataFieldRepository.findByTypeAndIncludeStatisticsTrue(MetadataField.FieldType.INTEGER);
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        for (MetadataField mf: integerFields) {
            List<String> result = queryFactory.select(QMetadataValue.metadataValue.value)
                    .from(QMetadataValue.metadataValue)
                    .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                    .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                    .where(getFiltersExpression(project, filters).and(QMetadataField.metadataField.eq(mf))).fetch();
            Map<String, Long> fieldStatistics = new LinkedHashMap<>();
            LongSummaryStatistics summaryStatistics = result.stream().mapToLong(value -> Long.parseLong(value)).summaryStatistics();
            fieldStatistics.put("Min", summaryStatistics.getMin());
            fieldStatistics.put("Max", summaryStatistics.getMax());
            fieldStatistics.put("Avg", (long) summaryStatistics.getAverage());
            statistics.put(mf.getName(), fieldStatistics);
        }
        return statistics;
    }

    private Map<String, Map<String, Long>> getStringMetadataStatistics(Project project, SampleFilterController.SampleFilters filters) {
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        List<Tuple> result = new ArrayList<>();
        if (filters.getTags() != null && filters.getTags().size() == 1){
            result = queryFactory.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QSample.sample.id.count())
                    .from(QMetadataValue.metadataValue)
                    .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                    .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                    .innerJoin(QAnnotation.annotation).on(QAnnotation.annotation.sample.eq(QMetadataValue.metadataValue.forA))
                    .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(getFiltersExpression(project, filters).and(QMetadataField.metadataField.includeStatistics.eq(true))
                            .and(QMetadataField.metadataField.type.eq(MetadataField.FieldType.STRING))
                            .and(QTag.tag.treePath.contains(filters.getTags().get(0).getTreePath())))
                    .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value).fetch();
        } else if (filters.getTags() != null && filters.getTags().size() > 1) {
            result = queryFactory.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QAnnotation.annotation.id)
                    .from(QMetadataValue.metadataValue)
                    .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                    .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                    .innerJoin(QAnnotation.annotation).on(QAnnotation.annotation.sample.eq(QMetadataValue.metadataValue.forA))
                    .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(getFiltersExpression(project, filters).and(QMetadataField.metadataField.includeStatistics.eq(true))
                            .and(QMetadataField.metadataField.type.eq(MetadataField.FieldType.STRING))
                            .and(controllerUtilities.getTagQuery(filters.getTags())))
                    .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value,
                            QAnnotation.annotation.sample.id, QAnnotation.annotation.start, QAnnotation.annotation.end)
                    .having(QTag.tag.name.countDistinct().eq(((long) filters.getTags().size())))
                    .fetch();
            return result.stream().collect(Collectors.groupingBy((Tuple tuple) -> tuple.get(QMetadataField.metadataField.name),
                    Collectors.groupingBy((Tuple tuple) -> tuple.get(QMetadataValue.metadataValue.value), Collectors.counting())));
        }
        result.forEach(qTuple -> {
            if (!statistics.containsKey(qTuple.get(QMetadataField.metadataField.name)))
                statistics.put(qTuple.get(QMetadataField.metadataField.name), new LinkedHashMap<>());
            statistics.get(qTuple.get(QMetadataField.metadataField.name)).put(qTuple.get(QMetadataValue.metadataValue.value), qTuple.get(2, Long.TYPE));
        });
        return statistics;
    }

    private Map<String, Map<String, Long>> getGlobalStringMetadataStatistics(Project project, SampleFilterController.SampleFilters filters) {
        Map<String, Map<String, Long>> statistics = new HashMap<>();
        List<Tuple> result = new ArrayList<>();
        if (filters.getTags() != null && filters.getTags().size() == 1){
            result = queryFactory.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QSample.sample.id.count())
                    .from(QMetadataValue.metadataValue)
                    .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                    .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                    .innerJoin(QAnnotation.annotation).on(QAnnotation.annotation.sample.eq(QMetadataValue.metadataValue.forA))
                    .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(QMetadataField.metadataField.includeStatistics.eq(true)
                            .and(QMetadataField.metadataField.type.eq(MetadataField.FieldType.STRING))
                            .and(QTag.tag.treePath.contains(filters.getTags().get(0).getTreePath())))
                    .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value).fetch();
        } else if (filters.getTags() != null && filters.getTags().size() > 1) {
            result = queryFactory.select(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value, QAnnotation.annotation.id)
                    .from(QMetadataValue.metadataValue)
                    .innerJoin(QMetadataValue.metadataValue.forA, QSample.sample)
                    .innerJoin(QMetadataValue.metadataValue.values, QMetadataField.metadataField)
                    .innerJoin(QAnnotation.annotation).on(QAnnotation.annotation.sample.eq(QMetadataValue.metadataValue.forA))
                    .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                    .where(QMetadataField.metadataField.includeStatistics.eq(true)
                            .and(QMetadataField.metadataField.type.eq(MetadataField.FieldType.STRING))
                            .and(controllerUtilities.getTagQuery(filters.getTags())))
                    .groupBy(QMetadataField.metadataField.name, QMetadataValue.metadataValue.value,
                            QAnnotation.annotation.sample.id, QAnnotation.annotation.start, QAnnotation.annotation.end)
                    .having(QTag.tag.name.countDistinct().eq(((long) filters.getTags().size())))
                    .fetch();
            return result.stream().collect(Collectors.groupingBy((Tuple tuple) -> tuple.get(QMetadataField.metadataField.name),
                    Collectors.groupingBy((Tuple tuple) -> tuple.get(QMetadataValue.metadataValue.value), Collectors.counting())));
        }
        result.forEach(qTuple -> {
            if (!statistics.containsKey(qTuple.get(QMetadataField.metadataField.name)))
                statistics.put(qTuple.get(QMetadataField.metadataField.name), new LinkedHashMap<>());
            statistics.get(qTuple.get(QMetadataField.metadataField.name)).put(qTuple.get(QMetadataValue.metadataValue.value), qTuple.get(2, Long.TYPE));
        });
        return statistics;
    }

    private int getTextOccurrences(String word, String text) {
        Pattern pattern = Pattern.compile("\\b" + word.replace("*", "[A-Za-zÀ-ÖØ-öø-ÿ]*") + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int sampleOccurrences = 0;
        while (matcher.find())
            sampleOccurrences++;
        return sampleOccurrences;
    }
}
