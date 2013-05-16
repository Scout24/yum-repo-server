package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.RepoType;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.util.monitoring.InApplicationMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;


@Controller
@RequestMapping("/repo")
@TimeMeasurement
public class RepositoryController {
  private final GridFsService gridFs;
  private final RepoService repoService;

  @Autowired
  public RepositoryController(GridFsService gridFs, RepoService repoService) {
    this.gridFs = gridFs;
    this.repoService = repoService;
  }

  @RequestMapping(value = "/{reponame}", method = POST, consumes = APPLICATION_FORM_URLENCODED_VALUE)
  @ResponseStatus(CREATED)
  public void uploadRpmViaCurl(@PathVariable("reponame") String reponame, HttpServletRequest request)
                        throws IOException {
    uploadRpm(reponame, request.getInputStream());
  }

  @RequestMapping(value = "/{reponame}", method = POST, consumes = MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(CREATED)
  public void uploadRpmViaRepoClient(@PathVariable("reponame") String reponame,
                                     @RequestPart("rpmFile") MultipartFile multipartFile) throws IOException {
    uploadRpm(reponame, multipartFile.getInputStream());
  }

  @RequestMapping(value = "/{reponame}", method = DELETE)
  @ResponseStatus(NO_CONTENT)
  public void deleteRepository(@PathVariable("reponame") String reponame) throws IOException {
    gridFs.deleteRepository(reponame);
    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".delete.repo");
  }

  @RequestMapping(value = "/{reponame}/type", method = PUT)
  @ResponseStatus(NO_CONTENT)
  public void updateRepositoryType(@PathVariable("reponame") String reponame, @RequestBody
                                   RepoType repoType) {
    repoService.setRepoType(reponame, repoType);
  }

  @RequestMapping(value = "/{reponame}/maxKeepRpms", method = PUT)
  @ResponseStatus(NO_CONTENT)
  public void updateMaxKeepRpms(@PathVariable("reponame") String reponame, @RequestBody
                                int maxKeepRpms) {
    repoService.setMaxKeepRpms(reponame, maxKeepRpms);
  }

  @RequestMapping(method = POST)
  @ResponseStatus(CREATED)
  public void createStaticRepository(@RequestParam("name") String reponame) {
    repoService.createOrUpdate(reponame);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(
    value = NOT_ACCEPTABLE, reason = "we dont know what you wanted, but your request is for us NOT_ACCEPTABLE"
  )
  public void onError() {
  }

  private void uploadRpm(String reponame, InputStream inputStream) {
    try {
      gridFs.storeRpm(reponame, inputStream);
    } catch (InvalidRpmHeaderException e) {
      throw new BadRequestException("Could not read RPM header.", e);
    } catch (IOException e) {
      throw new IllegalStateException("Could not save RPM.", e);
    }

    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".upload");
  }

}
