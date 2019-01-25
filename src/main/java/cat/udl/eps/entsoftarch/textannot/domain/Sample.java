package cat.udl.eps.entsoftarch.textannot.domain;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

@Entity
@Data
@Table(name = "TextAnnotSample")
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
    @NotNull
    @Lob
    @Type(type = "text")
    private String text;


    /**
     * Linking Sample with MetadataTemplate;
     */
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    private MetadataTemplate describedBy;


    /**
     * Linking Sample with TagHierarchy.
     */
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    private TagHierarchy taggedBy;

    public Sample(String text) {
        this.text=text;
    }

    public Sample() {
    }
}
