package cat.udl.eps.entsoftarch.textannot.domain.projections;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(
        name = "fieldValue",
        types = { MetadataValue.class }
)
public interface MetadataValueProjection {
    Integer getId();
    String getUri();
    String getValue();
    @Value("#{target.getValues().getCategory()}")
    String getFieldCategory();
    @Value("#{target.getValues().getName()}")
    String getFieldName();
}
