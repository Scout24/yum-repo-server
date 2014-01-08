package de.is24.infrastructure.gridfs.http.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseStatus;
import static org.springframework.http.HttpStatus.FORBIDDEN;


@ResponseStatus(FORBIDDEN)
public class ForbiddenException extends RuntimeException {
  private static final Logger LOG = LoggerFactory.getLogger(ForbiddenException.class);

  public ForbiddenException(String message, Throwable cause) {
    super(message, cause);
    LOG.warn("Got bad request: ", this);
  }

  public ForbiddenException(String message) {
    super(message);
    LOG.warn("Got bad request: ", this);
  }
}
