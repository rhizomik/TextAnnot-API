package cat.udl.eps.entsoftarch.textannot.repository;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ProjectRepository extends PagingAndSortingRepository<Project, Integer> {

    /**
     * Returns the Project found by a given name.
     * @param name The given name of Project.
     * @return Project object.
     */
    Project findByName(@Param("name") String name);
    List<Project> findByNameContaining(@Param("name") String name);
}