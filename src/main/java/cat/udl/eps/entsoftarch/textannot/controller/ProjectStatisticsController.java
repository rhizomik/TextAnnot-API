package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.exception.NotFoundException;
import cat.udl.eps.entsoftarch.textannot.repository.AnnotationRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cat.udl.eps.entsoftarch.textannot.service.StatisticsService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;
import java.util.Optional;

@BasePathAwareController
public class ProjectStatisticsController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("projects/{id}/statistics")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody ProjectStatistics getProjectStatistics(@PathVariable("id") Integer projectId) {
        Project project = getProjectOrThrowException(projectId);
        ProjectStatistics statistics = new ProjectStatistics();
        statistics.setTotalSamples(sampleRepository.countByProject(project));
        statistics.setTotalWords(sampleRepository.getTotalWordsCount(project));
        statistics.setAnnotatedSamples(sampleRepository.countAnnotatedSamples(project));
        statistics.setTotalAnnotations(annotationRepository.countAnnotationsByProject(project));
        statistics.setMetadataStatistics(statisticsService.getProjectMetadataStatistics(project));
        return statistics;
    }

    private Project getProjectOrThrowException(Integer projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if(!project.isPresent())
            throw new NotFoundException();
        return project.get();
    }

    @Data
    private static class ProjectStatistics {
        private long totalSamples;
        private long totalWords;
        private long annotatedSamples;
        private long totalAnnotations;
        private Map<String, Map<String, Long>> metadataStatistics;
    }
}
