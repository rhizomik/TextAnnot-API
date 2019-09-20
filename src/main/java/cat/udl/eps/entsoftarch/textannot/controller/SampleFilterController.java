package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.exception.NotFoundException;
import cat.udl.eps.entsoftarch.textannot.exception.UnauthorizedException;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cat.udl.eps.entsoftarch.textannot.service.ControllerUtilities;
import cat.udl.eps.entsoftarch.textannot.service.StatisticsService;
import com.querydsl.core.BooleanBuilder;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;

@BasePathAwareController
public class SampleFilterController {

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private ControllerUtilities controllerUtilities;

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
        if(tags != null && !tags.isEmpty() &&
                SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)
            throw new UnauthorizedException();

        Project project = getProjectOrThrowException(projectId);

        List<Tag> tagList = controllerUtilities.getTagsFromIds(tags);

        BooleanBuilder query = statisticsService.getFiltersExpression(project, new SampleFilters(word, getMetadataMap(params), tagList));
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
        List<Tag> tagList = controllerUtilities.getTagsFromIds(tags);
        SampleFilters filters = new SampleFilters(word, getMetadataMap(params), tagList);
        StatisticsResults statisticsResults = new StatisticsResults();
        if (filters.getTags() != null && !filters.getTags().isEmpty()) {
            statisticsResults.setMetadataStatistics(statisticsService.getMetadataStatistics(project, filters));
            statisticsResults.setGlobalMetadataStatistics(statisticsService.getGlobalMetadataStatistics(project, filters));
            Pair<Long, Long> globalCounts = statisticsService.getGlobalSampleCounts(project, filters);
            statisticsResults.setTotalOccurrences(globalCounts.getFirst());
            statisticsResults.setTotalSamples(globalCounts.getSecond());
        }
        Pair<Long, Long> counts = statisticsService.getSampleCounts(project, filters);
        statisticsResults.setOccurrences(counts.getFirst());
        statisticsResults.setSamples(counts.getSecond());
        return statisticsResults;
    }

    private Project getProjectOrThrowException(Integer projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if(!project.isPresent())
            throw new NotFoundException();
        return project.get();
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
        private List<Tag> tags;

        public SampleFilters() {
        }

        public SampleFilters(String word, Map<String, String> metadata, List<Tag> tags) {
            this.word = word;
            this.metadata = metadata;
            this.tags = tags;
        }
    }

}
