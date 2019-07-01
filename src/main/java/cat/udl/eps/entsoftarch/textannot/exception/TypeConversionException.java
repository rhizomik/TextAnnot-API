package cat.udl.eps.entsoftarch.textannot.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST, reason="This MetadataField has associated values that cannot be converted to the new type")
public class TypeConversionException extends RuntimeException {

    public TypeConversionException() {
        super();
    }
}
