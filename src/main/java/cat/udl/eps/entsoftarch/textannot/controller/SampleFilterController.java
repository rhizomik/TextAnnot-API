package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.exception.NotFoundException;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyValidationException;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cat.udl.eps.entsoftarch.textannot.service.StatisticsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Data;
import org.aspectj.weaver.ast.Not;
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
    private ProjectRepository projectRepository;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(em);
    }

    @GetMapping("/samples/filter")
    public @ResponseBody
    PagedResources<PersistentEntityResource> getFilteredSamples(@RequestParam("projectId") Integer projectId,
                                                                @RequestParam("word") String word,
                                                                @RequestParam("tags") List<String> tags,
                                                                @RequestParam Map<String, String> params,
                                                                Pageable pageable, PagedResourcesAssembler resourceAssembler) {

        Project project = getProjectOrThrowException(projectId);

        BooleanBuilder query = statisticsService.getFiltersExpression(project, new SampleFilters(word, getMetadataMap(params), tags));
        Page<Sample> samples = sampleRepository.findAll(query, pageable);

        if (!samples.hasContent()) {
            return resourceAssembler.toEmptyResource(samples, Sample.class);
        }

        return resourceAssembler.toResource(samples);
    }

    @GetMapping("/samples/filter/statistics")
    public @ResponseBody
    StatisticsResults getFilteredSamplesStatistics(@RequestParam("projectId") Integer projectId,
                                                   @RequestParam("word") String word,
                                                   @RequestParam("tags") List<String> tags,
                                                   @RequestParam Map<String, String> params) {
        Project project = getProjectOrThrowException(projectId);
        SampleFilters filters = new SampleFilters(word, getMetadataMap(params), tags);
        StatisticsResults statisticsResults = new StatisticsResults();
        statisticsResults.setMetadataStatistics(statisticsService.getMetadataStatistics(project, filters));
        statisticsResults.setGlobalMetadataStatistics(statisticsService.getGlobalMetadataStatistics(project, filters));
        Pair<Long, Long> counts = statisticsService.getSampleCounts(project, filters);
        statisticsResults.setOccurrences(counts.getFirst());
        statisticsResults.setSamples(counts.getSecond());
        Pair<Long, Long> globalCounts = statisticsService.getGlobalSampleCounts(project, filters);
        statisticsResults.setTotalOccurrences(globalCounts.getFirst());
        statisticsResults.setTotalSamples(globalCounts.getSecond());
        statisticsResults.setAnnotationStatistics(getAnnotationStatistics(project, filters));
        return statisticsResults;
    }

    private Project getProjectOrThrowException(Integer projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if(!project.isPresent())
            throw new NotFoundException();
        return project.get();
    }

    private List<AnnotationStatistics> getAnnotationStatistics(Project project, SampleFilters filters) {
        List<Tuple> result = queryFactory.select(QTag.tag.name, QTag.tag.treePath, QSample.sample.count(), QSample.sample.countDistinct())
                .from(QAnnotation.annotation)
                .innerJoin(QAnnotation.annotation.sample, QSample.sample)
                .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                .where(statisticsService.getFiltersExpression(project, filters))
                .groupBy(QTag.tag.name)
                .orderBy(QTag.tag.id.asc()).fetch();
        List<Tuple> globalResult = queryFactory.select(QTag.tag.name, QSample.sample.countDistinct())
                .from(QAnnotation.annotation)
                .innerJoin(QAnnotation.annotation.sample, QSample.sample)
                .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                .innerJoin(QSample.sample.project, QProject.project)
                .where(QProject.project.eq(project))
                .groupBy(QTag.tag.name).fetch();
        Map<String, Long> globalResultMap = globalResult.stream().collect(Collectors.toMap((Tuple t) -> t.get(QTag.tag.name), (Tuple t) -> t.get(1, Long.TYPE)));
        Map<String, AnnotationStatistics> annotationStatisticsMap = new HashMap<>();

         List<AnnotationStatistics> annotationStatisticsList = result.stream().map(tuple -> updateStatisticsTreeAndGetRoot(annotationStatisticsMap, tuple.get(QTag.tag.treePath),
                new AnnotationStatistics(tuple.get(QTag.tag.name), tuple.get(2, Long.class),
                        tuple.get(3, Long.class), globalResultMap.get(tuple.get(QTag.tag.name)))))
                 .distinct().collect(Collectors.toCollection(LinkedList::new));
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
        params.remove("projectId");
        return params;
    }

    @Data
    public static class StatisticsResults {
        private long occurrences;
        private long samples;
        private long totalOccurrences;
        private long totalSamples;
        private Map<String, Map<String, Long>> metadataStatistics;
        private Map<String, Map<String, Long>> globalMetadataStatistics;
        private List<AnnotationStatistics> annotationStatistics;
    }

    @Data
    public static class AnnotationStatistics {
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
    public static class SampleFilters implements Serializable {
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
