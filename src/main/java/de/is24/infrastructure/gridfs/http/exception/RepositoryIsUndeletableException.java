package de.is24.infrastructure.gridfs.http.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@ResponseStatus(FORBIDDEN)
public class RepositoryIsUndeletableException extends RuntimeException {
  public RepositoryIsUndeletableException(final String reponame, Throwable cause) {
    super(createMessageWith(reponame), cause);

  }

  public RepositoryIsUndeletableException(final String reponame) {
    super(createMessageWith(reponame));

  }

  private static String createMessageWith(String reponame) {
    return "The repository '" + reponame + "' is protected and can't be removed.";
  }
}
