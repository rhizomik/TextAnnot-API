package cat.udl.eps.entsoftarch.textannot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST, reason="Your request is not valid")
public class BadRequestException extends RuntimeException {
    public BadRequestException() {
        super();
    }
}
