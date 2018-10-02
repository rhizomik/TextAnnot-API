package cat.udl.eps.entsoftarch.textannot.domain;

import javax.persistence.*;

import cat.udl.eps.entsoftarch.textannot.domain.validator.RangeConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@RangeConstraint
public class Annotation extends UriEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NonNull
    private Integer start;

    @NonNull
    private Integer end;

    private Boolean reviewed;

    @ManyToOne
    private Sample annotated;

    @ManyToOne
    private Tag tagged;

    public Annotation(){

    }

    @Override
    public Integer getId() {
        return null;
    }
}
