package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.repos.RepositoryInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/repo/virtual")
@TimeMeasurement
public class VirtualRepositoryInfoController extends AbstractRepositoryInfoController {
  @Autowired
  public VirtualRepositoryInfoController(RepositoryInfoProvider virtualRepositoryInfoProvider,
                                         RepoService repoService) {
    super(virtualRepositoryInfoProvider, repoService, false);
  }


  @Override
  protected String getRepositoryInfoViewName() {
    return "virtualRepoInfo";
  }
}
