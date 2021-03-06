package cat.udl.eps.entsoftarch.textannot.domain;

import com.fasterxml.jackson.annotation.JsonIdentityReference;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Sample extends UriEntity<Integer>{

    /**
     * Identifier of sample needs to be unique, otherwise it will generate conflicts.
     */
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;


    /**
     * The text contained in the Sample. It is limited to 16KB and it can't be null.
     */
    @NotNull @NotBlank
    @Lob
    @Type(type = "text")
    private String text;

    private Integer wordCount;

    /**
     * Linking Sample with Project.
     */
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    private Project project;

    @ManyToMany
    private List<AnnotationStatus> annotationStatuses;

    public Sample(String text) {
        this.text=text;
    }

    public Sample() {
    }

    public void countWords() {
        if (text != null)
            wordCount = text.split("[\\s\\n]").length;
    }
}
