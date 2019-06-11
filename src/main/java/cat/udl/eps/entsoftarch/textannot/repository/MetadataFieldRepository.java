package cat.udl.eps.entsoftarch.textannot.repository;


import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface MetadataFieldRepository extends PagingAndSortingRepository<MetadataField, Integer> {
    /**
     * Returns the metadataField found by a given name.
     * @param name The given name of metadataField.
     * @return metadataField object.
     */
    MetadataField findByName (@Param("name") String name);

    /**
     * Returns the metadataField found by a given name and a given category.
     * @param category The given category of metadataField.
     * @param name The given name of metadataField.
     * @return metadataField object.
     */
    MetadataField findByCategoryAndName (@Param("category") String category, @Param("name") String name);

    /**
     * Returns the metadataField list related to a metadataTemplate.
     * @param project The given metadataTemplate that contains metadataFields that we want.
     * @return a list of metadataFields.
     */
    List<MetadataField> findByDefinedAt(@Param("project") Project project);
}
