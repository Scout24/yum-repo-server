package de.is24.infrastructure.gridfs.http.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import static org.springframework.http.HttpStatus.CONFLICT;


@ResponseStatus(value = CONFLICT, reason = "RPM already exists in target repository")
public class GridFSFileAlreadyExistsException extends RuntimeException {
  public GridFSFileAlreadyExistsException(String message, final String path, Throwable cause) {
    super(createMessageWith(message, path), cause);
  }

  public GridFSFileAlreadyExistsException(String message, final String path) {
    super(createMessageWith(message, path));
  }

  private static String createMessageWith(String message, String path) {
    return message.concat(" Path: " + path);
  }
}
