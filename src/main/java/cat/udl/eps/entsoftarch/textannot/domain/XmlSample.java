package cat.udl.eps.entsoftarch.textannot.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.Type;

@Entity
@Data
public class XmlSample extends Sample {

    /**
     * The content contained in the XmlSample. It is limited to 16KB and it can't be null.
     */
    @NotNull
    @Lob
    @Type(type = "text")
    private String content;

    /**
     * Sets the content of the XmlSample. If text is null sets empty content.
     * If first character is invalid, it removes it.
     * @param content
     */
    public void setContent(String content) {
        if (getText() == null) setText("");
        if (content.charAt(0) == '\uFEFF') // Remove invalid char
            this.content = content.substring(1);
        else
            this.content = content;
    }
}
