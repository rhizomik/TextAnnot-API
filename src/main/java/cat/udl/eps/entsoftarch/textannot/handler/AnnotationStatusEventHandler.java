package cat.udl.eps.entsoftarch.textannot.handler;

import cat.udl.eps.entsoftarch.textannot.domain.Annotation;
import cat.udl.eps.entsoftarch.textannot.domain.AnnotationStatus;
import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.exception.AnnotationException;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.stream.Collectors;

@Component
@RepositoryEventHandler
public class AnnotationStatusEventHandler {
    final Logger logger = LoggerFactory.getLogger(Annotation.class);

    @Autowired
    SampleRepository sampleRepository;

    @HandleBeforeDelete
    @Transactional
    public void handleAnnotationStatusPreDelete(AnnotationStatus annotationStatus) throws AnnotationException {
        logger.info("Before creating: {}", annotationStatus.toString());
        Page<Sample> samples = sampleRepository.findByAnnotationStatuses(annotationStatus, Pageable.unpaged());
        samples.forEach(sample -> {
            sample.setAnnotationStatuses(sample.getAnnotationStatuses().stream().filter(sampleStatus -> !sampleStatus.equals(annotationStatus)).collect(Collectors.toList()));
            sampleRepository.save(sample);
        });
    }


}
