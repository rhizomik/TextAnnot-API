package cat.udl.eps.entsoftarch.textannot.repository;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.domain.projections.MetadataValueProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;

import java.util.List;

@RepositoryRestResource(excerptProjection = MetadataValueProjection.class)
public interface MetadataValueRepository extends PagingAndSortingRepository<MetadataValue, Integer> {
    /**
     * Returns the metadataField found by a given value.
     * @param value The given value of metadataField.
     * @return metadataValue object.
     */
    MetadataValue findByValue(@Param("value") String value);

    /**
     * Returns a list of metadataValue that were found by a given value.
     * @param value The given value of a group of metadataValue.
     * @return list of metadataValues.
     */
    List<MetadataValue> findByValueContaining(@Param("value") String value);

    /**
     * Returns a list of metadataValue that were found by a given metadataField.
     * @param metadataField The given metadataField of a group of metadataValue.
     * @return list of metadataValues.
     */
    List<MetadataValue> findByValues(@Param("metadataField")MetadataField metadataField);

    /**
     * Returns a list of metadataValue that were found by a given sample.
     * @param sample The given sample of a group of metadataValues
     * @return list of metadataValues.
     */
    @PostFilter("hasRole('ADMIN') || !filterObject.values.privateField")
    List<MetadataValue> findByForA(@Param("sample") Sample sample);
}
