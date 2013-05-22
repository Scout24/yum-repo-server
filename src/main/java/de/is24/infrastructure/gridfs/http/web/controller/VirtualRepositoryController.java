package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.VIRTUAL;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@RequestMapping("/repo/virtual")
@TimeMeasurement
public class VirtualRepositoryController {
  private final GridFsService gridFs;
  private final RepoService repoService;
  private final FileController fileController;

  public VirtualRepositoryController() {
    this.gridFs = null;
    this.repoService = null;
    this.fileController = null;
  }

  @Autowired
  public VirtualRepositoryController(GridFsService gridFs, RepoService repoService, FileController fileController) {
    this.gridFs = gridFs;
    this.repoService = repoService;
    this.fileController = fileController;
  }

  @RequestMapping(method = POST)
  @ResponseStatus(CREATED)
  public void createVirtualRepo(@RequestParam("name") String reponame,
                                @RequestParam("destination") String destination) throws IOException {
    repoService.createVirtualRepo(reponame, destination);
  }

  @RequestMapping(value = "/{reponame}", method = DELETE)
  @ResponseStatus(NO_CONTENT)
  public void deleteVirtualRepo(@PathVariable("reponame") String reponame) {
    repoService.deleteVirtual(reponame);
  }

  @RequestMapping(value = "/{repo}/{arch}/{filename:.+}", method = GET)
  public ResponseEntity<InputStreamResource> deliverFile(@PathVariable("repo") String repo,
                                                         @PathVariable("arch") String arch,
                                                         @PathVariable("filename") String filename) throws IOException {
    return deliverFileInternal(repo, arch, filename, null);
  }

  @RequestMapping(value = "/{repo}/{arch}/{filename:.+}", method = GET, headers = { "Range" })
  public ResponseEntity<InputStreamResource> deliverRangeOfFile(@PathVariable("repo") String repo,
                                                                @PathVariable("arch") String arch,
                                                                @PathVariable("filename") String filename,
                                                                @RequestHeader("Range") String rangeHeader)
                                                         throws IOException {
    return deliverFileInternal(repo, arch, filename, rangeHeader);
  }

  private ResponseEntity<InputStreamResource> deliverFileInternal(String repo, String arch, String filename,
                                                                  String rangeHeader) throws IOException {
    RepoEntry entry = repoService.getRepo(repo, VIRTUAL);
    if (entry.isExternal()) {
      return createRedirect(getExternalUri(entry.getTarget(), arch, filename));
    }

    if (rangeHeader != null) {
      return fileController.deliverRangeOfFile(entry.getTarget(), arch, filename, rangeHeader);
    }

    return fileController.deliverFile(entry.getTarget(), arch, filename);
  }

  private ResponseEntity<InputStreamResource> createRedirect(URI uri) {
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(uri);
    return new ResponseEntity<>(headers, HttpStatus.valueOf(302));
  }

  private URI getExternalUri(String target, String arch, String filename) {
    String url = target.endsWith("/") ? target : (target + "/");
    url += arch + "/" + filename;

    URI location;
    try {
      location = new URI(url);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Could not build external target url.");
    }
    return location;
  }

}
