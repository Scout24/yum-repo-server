package de.is24.infrastructure.gridfs.http.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class RepositoryNotFoundException extends RuntimeException {
  public RepositoryNotFoundException(String message, final String reponame, Throwable cause) {
    super(createMessageWith(message, reponame), cause);

  }

  public RepositoryNotFoundException(String message, final String reponame) {
    super(createMessageWith(message, reponame));

  }

  private static String createMessageWith(String message, String reponame) {
    return message.concat(" Repository: " + reponame);
  }
}
