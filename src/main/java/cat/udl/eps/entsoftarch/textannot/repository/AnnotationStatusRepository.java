package cat.udl.eps.entsoftarch.textannot.repository;

import cat.udl.eps.entsoftarch.textannot.domain.AnnotationStatus;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface AnnotationStatusRepository extends PagingAndSortingRepository<AnnotationStatus, Integer> {
    List<AnnotationStatus> findByDefinedAt(@Param("project") Project project);

    AnnotationStatus findByNameAndDefinedAt(String name, Project project);
}
