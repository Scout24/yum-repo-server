package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.metadata.YumEntriesHashCalculator;
import de.is24.util.monitoring.spring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.repos.RepositoryInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/repo")
@TimeMeasurement
public class StaticRepositoryInfoController extends AbstractRepositoryInfoController {
  @Autowired
  public StaticRepositoryInfoController(RepositoryInfoProvider staticRepositoryInfoProvider, RepoService repoService, YumEntriesHashCalculator yumEntriesHashCalculator) {
    super(staticRepositoryInfoProvider, repoService, yumEntriesHashCalculator, true);
  }

  @Override
  protected String getRepositoryInfoViewName() {
    return "staticRepoInfo";
  }
}
