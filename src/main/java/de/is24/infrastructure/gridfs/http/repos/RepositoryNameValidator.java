package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import static org.apache.commons.lang.StringUtils.isBlank;


public final class RepositoryNameValidator {
  private static final String REPO_REGEX = "^[a-zA-Z0-9-_.]+$";

  private RepositoryNameValidator() {
  }

  public static void validateRepoName(String destinationRepo) {
    if (isBlank(destinationRepo)) {
      throw new BadRequestException("Destination repo is not allowed to be blank!");
    }
    if (!destinationRepo.matches(REPO_REGEX)) {
      throw new BadRequestException("Destination repo is not valid!");
    }
  }
}
