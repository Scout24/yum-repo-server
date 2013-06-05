package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.io.IOException;
import java.sql.SQLException;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@TimeMeasurement
public class MetadataController {
  private final MetadataService metadataService;
  private final RepoService repoService;

  @Autowired
  public MetadataController(MetadataService metadataService, RepoService repoService) {
    this.metadataService = metadataService;
    this.repoService = repoService;
  }

  public MetadataController() {
    this.metadataService = null;
    this.repoService = null;
  }

  private static final Logger LOG = LoggerFactory.getLogger(MetadataController.class);

  @RequestMapping(value = "/repo/{reponame}/repodata", method = POST)
  @ResponseStatus(CREATED)
  public void generate(@PathVariable("reponame") String reponame) throws IOException, SQLException {
    LOG.info("Generate metadata for repository: {}", reponame);

    if (repoService.isRepoScheduled(reponame)) {
      responseWithError(reponame);
    } else {
      generateMetaData(reponame);
    }

    LOG.info("Metadata generation for {} done.", reponame);
  }

  private void responseWithError(String reponame) {
    throw new BadRequestException(
      "It is not allowed to manually generate metadata for scheduled repositories, scheduled repo: '" + reponame + "'");
  }

  private void generateMetaData(String reponame) throws IOException, SQLException {
    metadataService.generateYumMetadata(reponame);
  }
}
