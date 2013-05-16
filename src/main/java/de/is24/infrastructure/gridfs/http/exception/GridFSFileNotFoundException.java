package de.is24.infrastructure.gridfs.http.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class GridFSFileNotFoundException extends RuntimeException {
  public GridFSFileNotFoundException(String message, final String path, Throwable cause) {
    super(createMessageWith(message, path), cause);

  }

  public GridFSFileNotFoundException(String message, final String path) {
    super(createMessageWith(message, path));

  }

  private static String createMessageWith(String message, String path) {
    return message.concat(" Path: " + path);
  }
}
