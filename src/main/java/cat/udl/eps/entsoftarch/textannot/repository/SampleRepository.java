package cat.udl.eps.entsoftarch.textannot.repository;

import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource
public interface SampleRepository extends PagingAndSortingRepository<Sample, Integer>, QuerydslPredicateExecutor<Sample> {

    /**
     * Finds all Samples that has text contained like the given one
     * @param word String text for finding in Samples
     * @return The list of Samples that contains text in given parameter
     */
    @Query(nativeQuery = true, value = "SELECT id FROM sample s WHERE s.text REGEXP ?#{'[[:<:]]' + #word + '[[:>:]]'}")
    List<Integer> findByTextContainingWord(@Param("word") String word);

    /**
     * Finds all Samples that has word contained like the given one
     * @param word String text for finding in Samples
     * @return The list of Samples that contains word in given parameter
     */
    List<Sample> findByTextContains (@Param("word") String word);

    /**
     * Query that gives us a list of Samples that accomplish that his id is equal to Metadatavalue id
     and Metadatavalue id is equal to MetadataField id
     and his value is equal to the second parameter(String value)
     and his name is equal to the first parameter (String name).
     * @param value The given value of Sample
     * @param name The given name of Sample
     * @return a list of Samples that accomplishes the result of the query.
     */

    @Query("SELECT s " +
            "FROM Sample s, MetadataValue v, MetadataField f " +
            "WHERE v.forA.id = s.id AND  v.values.id = f.id AND v.value = ?2 AND f.name = ?1")
    List<Sample> findAllSamplesWithFieldNameAndValue(@Param("name") String name, @Param("value") String value);


    /**
     * Query that gives us a list of Samples that accomplish that his id is equal to Metadatavalue id
     and Metadatavalue id is equal to MetadataField id
     and his name is equal to the first parameter (String name).
     * @param name The given name of Sample
     * @return a list of Samples that accomplishes the result of the query.
     */

    @Query("SELECT s " +
            "FROM Sample s, MetadataValue v, MetadataField f " +
            "WHERE v.forA.id = s.id AND  v.values.id = f.id AND f.name = ?1")
    List<Sample> findAllSamplesWithFieldName(@Param("name") String name);


    /**
     * Finds a Sample that has text contained like the given one
     * @param text String text for finding in Samples
     * @return The Sample object that contains text in given parameter
     */
    Sample findByText(@Param("text") String text);


    /**
     * Finds all Samples that are contained in a project with name
     * @param name String text, name of the Project, for finding in Samples
     * @return The list of Samples that are described by same name as the parameter
     */

    List<Sample> findByProjectName(@Param("name") String name);

    /**
     Returns the Samples related to a project.
     * @param project The project that contains the Samples we want.
     * @return list of samples.
     */
    Page<Sample> findByProject(@Param("project") Project project, Pageable pageable);

    @Query(value = "select s.* from sample s where s.project_id = ?#{#project.id} and " +
            "not exists (select a.id from annotation a where a.sample_id = s.id)",
    countQuery = "select count(s.id) from sample s where s.project_id = ?#{#project.id} and " +
            "not exists (select a.id from annotation a where a.sample_id = s.id)",
    nativeQuery = true)
    Page<Sample> findByProjectAndNotAnnotated(@Param("project") Project project, Pageable pageable);

    @Query("select coalesce(sum(s.wordCount), 0) from Sample s where s.project.id = ?#{#project.id}")
    @RestResource(exported = false)
    Integer getTotalWordsCount(@Param("project") Project project);

    @RestResource(exported = false)
    Integer countByProject(Project project);

    @Query("select count(distinct a.sample.id) from Annotation a inner join a.sample s where s.project.id = ?#{#project.id}")
    @RestResource(exported = false)
    Integer countAnnotatedSamples(@Param("project") Project project);
}