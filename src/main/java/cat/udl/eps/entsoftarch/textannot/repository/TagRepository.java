package cat.udl.eps.entsoftarch.textannot.repository;

import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.transaction.Transactional;
import java.util.List;

@RepositoryRestResource
public interface TagRepository extends PagingAndSortingRepository<Tag, Integer>, QuerydslPredicateExecutor<Tag> {

    /**
     * Finds all Tags that has text containing like the given one
     * @param name String name for finding in Tags
     * @return The list of Tags that contains text in given parameter
     */
    List<Tag> findByNameContaining(@Param("name") String name);

    Tag findByName(@Param("name") String name);

    /**
     Returns the Tags related to a project.
     * @param project The project that contains the Tags we want.
     * @return list of Tags.
     */
    List<Tag> findByProject(@Param("project") Project project);

    List<Tag> findByProjectAndParentIsNull(@Param("project") Project project);

    /**
     Returns the Tags related to a Tag parent.
     * @param parent The Tag parent that contains the Tag childs we want.
     * @return list of Tags.
     */

    List<Tag> findByParent(@Param("parent") Tag parent);

    List<Tag> findByParentId(@Param("id") Integer id);

    @RestResource(exported = false)
    void deleteByProject(Project project);
}
