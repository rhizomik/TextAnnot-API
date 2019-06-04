package cat.udl.eps.entsoftarch.textannot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND, reason="The object you were retrieving is not found")
public class NotFoundException extends RuntimeException {
    public NotFoundException() {
        super();
    }
}
