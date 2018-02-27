package cat.udl.eps.entsoftarch.textannot.domain;


import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
public class MetadataValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank
    private String value;

    public MetadataValue(){}

    public MetadataValue(String value){
        this.value = value;
    }
}
