package cat.udl.eps.entsoftarch.textannot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.UNAUTHORIZED, reason="You are not authorized to do this operation")
public class UnauthorizedException extends RuntimeException{
}
