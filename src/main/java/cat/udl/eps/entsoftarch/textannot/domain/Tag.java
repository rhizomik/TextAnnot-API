package cat.udl.eps.entsoftarch.textannot.domain;

import cat.udl.eps.entsoftarch.textannot.domain.validator.TagConstraint;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@TagConstraint
@Data
@EqualsAndHashCode(callSuper = true)
public class Tag extends UriEntity<Integer> {

    /**
     * Identifier of Tag needs to be unique, otherwise it will generate conflicts.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    /**
     *  The name given to the Tag. It can't be blank.
     *
     */

    @NotBlank
    private String name;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String treePath;

    /**
     * Linking Tag with Project.
     */
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    private Project project;

    /**
     * Creating a parent child relationship.
     */
    @ManyToOne
    private Tag parent;


    public Tag(String name) {
        this.setName(name);
    }

    public Tag(String name, Project project){
        this.setProject(project);
        this.setName(name);
    }
}
