package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import java.util.List;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping("/repo")
@TimeMeasurement
public class RpmInfoController {
  private final YumEntriesRepository yumEntriesRepository;

  public RpmInfoController() {
    yumEntriesRepository = null;
  }

  @Autowired
  public RpmInfoController(YumEntriesRepository yumEntriesRepository) {
    this.yumEntriesRepository = yumEntriesRepository;
  }

  @RequestMapping(value = "/{repo}/{arch}/{filename}/info.html", method = GET)
  public ModelAndView rpmInfo(@PathVariable("repo") String repo,
                              @PathVariable("arch") String arch,
                              @PathVariable("filename") String filename) {
    List<YumEntry> entries = yumEntriesRepository.findByRepoAndYumPackageLocationHref(repo, arch + "/" + filename);
    YumPackage yumPackage = getUniqueEntry(entries, repo + "/" + arch + "/" + filename).getYumPackage();
    return new ModelAndView("rpmInfo", "model", yumPackage);
  }

  private YumEntry getUniqueEntry(List<YumEntry> entries, String path) {
    if (entries.isEmpty()) {
      throw new GridFSFileNotFoundException("Could not find metadata.", path);
    }

    if (entries.size() > 1) {
      throw new IllegalStateException("More than one metadata entry found for " + path);
    }

    return entries.get(0);
  }

}
