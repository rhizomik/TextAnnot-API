package cat.udl.eps.entsoftarch.textannot.handler;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.MetadataValue;
import cat.udl.eps.entsoftarch.textannot.exception.TypeConversionException;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataValueRepository;
import javassist.tools.web.BadHttpRequest;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

@Component
@RepositoryEventHandler
public class MetadataFieldEventHandler {
    final Logger logger = LoggerFactory.getLogger(MetadataField.class);

    @Autowired
    MetadataValueRepository metadataValueRepository;

    @Autowired
    MetadataFieldRepository metadataFieldRepository;

    @Autowired
    EntityManager entityManager;

    @HandleBeforeSave
    @Transactional
    public void handleMetadataFieldPreSave(MetadataField metadataField) throws BadHttpRequest {
        logger.info("Before updating: {}", metadataField.toString());
        checkFieldTypeChange(metadataField);
    }

    private void checkFieldTypeChange(MetadataField metadataField) {
        entityManager.detach(metadataField);
        MetadataField oldMetadataField = metadataFieldRepository.findById(metadataField.getId()).get();
        if(oldMetadataField.getType().equals(metadataField.getType()) || metadataField.getType().equals(MetadataField.FieldType.STRING)) return;
        List<MetadataValue> fieldValues = metadataValueRepository.findByValues(oldMetadataField);
        if (!fieldValues.stream().allMatch(metadataValue -> checkValueType(metadataValue.getValue(), metadataField.getType()))) {
            throw new TypeConversionException();
        }
    }

    private boolean checkValueType(String value, MetadataField.FieldType type) {
        switch (type) {
            case INTEGER:
                return NumberUtils.isParsable(value);
            case DATE:
                return false;
            case BOOLEAN:
                return value.toUpperCase().equals("TRUE") || value.toUpperCase().equals("FALSE");
        }
        return false;
    }
}
