package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoTaggingService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.io.IOException;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


/**
 * @author twalter
 * @since 4/8/13
 */
@Controller
@RequestMapping("/repo")
@TimeMeasurement
public class RepositoryTaggingController {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryTaggingController.class);

  private final RepoTaggingService taggingService;

  @Autowired
  public RepositoryTaggingController(RepoTaggingService taggingService) {
    this.taggingService = taggingService;
  }

  @RequestMapping(value = "/{reponame}/tags", method = POST)
  @ResponseStatus(CREATED)
  public void addTag(@PathVariable("reponame") String reponame,
                     @RequestParam(value = "tag") String tagname) throws IOException {
    taggingService.addTag(reponame, tagname);
  }

  @RequestMapping(value = "/{reponame}/tags", method = GET, produces = TEXT_PLAIN_VALUE)
  @ResponseBody
  public String getTags(@PathVariable("reponame") String reponame) {
    return StringUtils.join(taggingService.getTags(reponame), "\n");
  }

  @RequestMapping(value = "/{reponame}/tags", method = DELETE)
  @ResponseStatus(NO_CONTENT)
  public void deleteAllTags(@PathVariable("reponame") String reponame) throws IOException {
    taggingService.deleteAllTags(reponame);
  }

  @RequestMapping(value = "/{reponame}/tags/{tagname}", method = DELETE)
  @ResponseStatus(NO_CONTENT)
  public void deleteTag(@PathVariable("reponame") String reponame,
                        @PathVariable("tagname") String tagname) throws IOException {
    taggingService.deleteTag(reponame, tagname);
  }

}
