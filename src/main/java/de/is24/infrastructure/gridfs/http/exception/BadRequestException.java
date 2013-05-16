package de.is24.infrastructure.gridfs.http.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class BadRequestException extends RuntimeException {

  private static final Logger LOG = LoggerFactory.getLogger(BadRequestException.class);

  public BadRequestException(String message, Throwable cause) {
    super(message, cause);
    LOG.warn("Got bad request: ", this);
  }

  public BadRequestException(String message) {
    super(message);
    LOG.warn("Got bad request: ", this);
  }
}
