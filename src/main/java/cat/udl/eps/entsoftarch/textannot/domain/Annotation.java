package cat.udl.eps.entsoftarch.textannot.domain;

import javax.persistence.*;

import cat.udl.eps.entsoftarch.textannot.domain.validator.RangeConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@RangeConstraint
public class Annotation extends UriEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    /**
     * Identifier of annotation needs to be unique, otherwise it will generate conflicts.
     */
    private Integer id;

    @NonNull
    /**
     * Indicates the start of the annotation. It must not be null and not bigger than end.
     */
    private Integer start;

    @NonNull
    /**
     * Indicates the end of the annotation. It must not be null and not smaller than start.
     */
    private Integer end;

    /**
     * Contains an information about is Annotation reviewed or not.
     */
    private Boolean reviewed = false;

    @ManyToOne
    @NonNull
    /**
     * Linking Annotation with Sample entity
     * NonNull checked by RangeConstraintValidator
     */
    private Sample sample;

    @ManyToOne
    /**
     *  Linking Annotation with Tag entity
     */
    private Tag tag;

    @ManyToOne
    private Linguist linguist;
}
