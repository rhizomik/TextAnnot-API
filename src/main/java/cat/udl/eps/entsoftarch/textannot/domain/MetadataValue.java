package cat.udl.eps.entsoftarch.textannot.domain;


import com.fasterxml.jackson.annotation.JsonIdentityReference;
import javax.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataValue extends UriEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    /**
     * Identifier of annotation needs to be unique, otherwise it will generate conflicts.
     */
    private Integer id;

    @NotBlank
    /**
     * Indicates the String value of the  Metadata value.
     */
    @Lob
    @Type(type = "text")
    private String value;

    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    /**
     * Linking MetadataValue with a Sample.
     */
    private	Sample forA;

    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    /**
     * Linking MetadataValue with MetadataField.
     */
    private	MetadataField values;

    public MetadataValue(String value){
        this.value = value;
    }

    public MetadataValue(){
    }

    @Override
    public Integer getId() { return id; }
}
