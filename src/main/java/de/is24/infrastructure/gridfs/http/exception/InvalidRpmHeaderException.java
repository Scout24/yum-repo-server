package de.is24.infrastructure.gridfs.http.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


@ResponseStatus(BAD_REQUEST)
public class InvalidRpmHeaderException extends Exception {
  public InvalidRpmHeaderException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidRpmHeaderException(String message) {
    super(message);
  }
}
