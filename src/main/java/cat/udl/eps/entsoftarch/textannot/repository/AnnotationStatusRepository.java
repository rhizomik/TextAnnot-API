package cat.udl.eps.entsoftarch.textannot.repository;

import cat.udl.eps.entsoftarch.textannot.domain.Annotation;
import cat.udl.eps.entsoftarch.textannot.domain.AnnotationStatus;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface AnnotationStatusRepository extends PagingAndSortingRepository<AnnotationStatus, Integer> {

    AnnotationStatus findByNameAndDefinedAt(String name, Project project);
}
