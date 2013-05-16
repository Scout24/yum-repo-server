package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.BadRangeRequestException;
import de.is24.infrastructure.gridfs.http.gridfs.BoundedGridFsResource;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;
import static org.springframework.http.MediaType.valueOf;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping(FileController.PREFIX)
@TimeMeasurement
public class FileController {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

  public static final String PREFIX = "/repo";
  private static final int PREFIX_LENGTH = PREFIX.length() + 1;
  public static final String RANGE_PATTERN_REGEXP = "^bytes=(0|[1-9]\\d*)-((0|[1-9]\\d*)?)$";
  private static final Pattern RANGE_PATTERN = compile(RANGE_PATTERN_REGEXP);
  public static final String RPM_EXTENSION = ".rpm";

  private final GridFsService gridFs;

  // just for cglib
  FileController() {
    this.gridFs = null;
  }

  @Autowired
  public FileController(GridFsService gridFs) {
    this.gridFs = gridFs;
  }

  @RequestMapping(value = "/{repo}/{arch}/{filename:.+}", method = GET)
  public ResponseEntity<InputStreamResource> deliverFile(@PathVariable("repo") String repo,
                                                         @PathVariable("arch") String arch,
                                                         @PathVariable("filename") String filename) throws IOException {
    BoundedGridFsResource resource = gridFs.getResource(repo + "/" + arch + "/" + filename);
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentLength(resource.contentLength());
    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".get.rpm");
    return new ResponseEntity<InputStreamResource>(resource, withContentType(httpHeaders, resource), OK);
  }

  @RequestMapping(value = "/{repo}/{arch}/{filename:.+}", method = GET, headers = { "Range" })
  public ResponseEntity<InputStreamResource> deliverRangeOfFile(@PathVariable("repo") String repo,
                                                                @PathVariable("arch") String arch,
                                                                @PathVariable("filename") String filename,
                                                                @RequestHeader("Range") String rangeHeader)
                                                         throws IOException {
    String path = repo + "/" + arch + "/" + filename;
    Matcher matcher = getMatcher(rangeHeader);
    String intervalStartString = matcher.group(1);
    String intervalEndString = matcher.group(2);

    long intervalStart = parseRangeLong(intervalStartString, rangeHeader);

    BoundedGridFsResource resource;
    if (isEmpty(intervalEndString)) {
      resource = gridFs.getResource(path, intervalStart);
    } else {
      long intervalEnd = parseRangeLong(intervalEndString, rangeHeader);
      if (intervalEnd < intervalStart) {
        throw new BadRangeRequestException(
          format("Range end is before range start for path [%s]", path),
          rangeHeader);
      }
      resource = gridFs.getResource(path, intervalStart, intervalEnd - intervalStart + 1);
    }

    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".get.rpm-range");
    return new ResponseEntity<InputStreamResource>(resource, withContentType(rangeHeaders(resource), resource),
      PARTIAL_CONTENT);
  }

  private long parseRangeLong(String value, String rangeHeader) {
    try {
      return parseLong(value);
    } catch (NumberFormatException e) {
      throw new BadRangeRequestException("Could not parse range element '" + value + "' to long.", rangeHeader);
    }
  }

  private HttpHeaders withContentType(HttpHeaders httpHeaders, BoundedGridFsResource resource) {
    if (isNotBlank(resource.getContentType())) {
      httpHeaders.setContentType(valueOf(resource.getContentType()));
    }
    return httpHeaders;
  }

  private Matcher getMatcher(String rangeHeader) {
    Matcher matcher = RANGE_PATTERN.matcher(rangeHeader);
    if (!matcher.matches()) {
      throw new BadRangeRequestException("Byte range header does not match " + RANGE_PATTERN_REGEXP, rangeHeader);
    }
    return matcher;
  }


  @RequestMapping(value = "/{repoName}/{arch}/{filename}" + RPM_EXTENSION, method = DELETE)
  public void deleteFile(@PathVariable("repoName") String repoName,
                         @PathVariable("arch") String arch,
                         @PathVariable("filename") String filename, HttpServletResponse response) {
    String path = join(asList(repoName, arch, filename + RPM_EXTENSION), "/");
    gridFs.delete(path);
    response.setStatus(SC_NO_CONTENT);
    LOGGER.info("Deleted file {}.rpm", filename);
    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".delete.rpm");
  }

  private HttpHeaders rangeHeaders(BoundedGridFsResource resource) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept-Ranges", "bytes");
    headers.set("Content-Range",
      "bytes 0-" + (resource.contentLength() - 1) + "/" + resource.getFileLength());
    headers.setContentLength(resource.contentLength());
    return headers;
  }

}
