package de.is24.infrastructure.gridfs.http.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseStatus;
import static org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;


@ResponseStatus(REQUESTED_RANGE_NOT_SATISFIABLE)
public class BadRangeRequestException extends RuntimeException {
  private static final Logger LOG = LoggerFactory.getLogger(BadRangeRequestException.class);

  public BadRangeRequestException(String message) {
    super(message);
    LOG.warn("Got bad range request: ", this);
  }

  public BadRangeRequestException(String message, final String originalHeader, Throwable cause) {
    super(createMessageWith(message, originalHeader), cause);
    LOG.warn("Got bad range request: ", this);
  }

  public BadRangeRequestException(String message, final String originalHeader) {
    super(createMessageWith(message, originalHeader));
    LOG.warn("Got bad range request: ", this);
  }

  private static String createMessageWith(String message, String originalHeader) {
    return message.concat(" -- header was: " + originalHeader);
  }
}
