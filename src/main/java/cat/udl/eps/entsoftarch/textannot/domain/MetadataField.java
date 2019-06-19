package cat.udl.eps.entsoftarch.textannot.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class MetadataField extends UriEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NotBlank
    private String name;

    private String xmlName;

    private String type;

    private String category;

    private Boolean includeStatistics;

    private Boolean privateField;

    @ManyToOne
    /**
     * Linking MetadataField with Metadata Template.
     */
    private Project definedAt;

    public MetadataField() {
        this.name = "";
    }

    public MetadataField(String name, String type){
        this.name = name;
        this.type = type;
    }
}
