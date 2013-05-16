package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FileInfo;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.SortField;
import de.is24.infrastructure.gridfs.http.domain.SortOrder;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.repos.RepositoryInfoProvider;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static ch.lambdaj.Lambda.joinFrom;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Maps.newHashMap;
import static de.is24.infrastructure.gridfs.http.domain.SortOrder.asc;
import static de.is24.infrastructure.gridfs.http.domain.SortOrder.none;
import static org.apache.commons.lang.WordUtils.capitalize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


public abstract class AbstractRepositoryInfoController {
  private static final int DEFAULT_OFFSET_STARTING_POINT = 5000;
  private static final int DEFAULT_OFFSET_END_POINT = -1;
  protected final RepositoryInfoProvider infoProvider;
  protected final RepoService repoService;
  protected final boolean isStatic;

  protected AbstractRepositoryInfoController(RepositoryInfoProvider infoProvider, RepoService repoService,
                                             boolean isStatic) {
    this.infoProvider = infoProvider;
    this.repoService = repoService;
    this.isStatic = isStatic;
  }

  @RequestMapping(method = GET, produces = TEXT_HTML_VALUE)
  public ModelAndView getRepositoriesAsHtml(
    @RequestParam(value = "search", required = false) String searchBy,
    @RequestParam(value = "sortBy", required = false, defaultValue = "name") SortField sortBy,
    @RequestParam(value = "order", required = false, defaultValue = "asc") SortOrder sortOrder) {
    Map<String, Object> model = new HashMap<>();
    model.putAll(sorting(sortBy, sortOrder));
    model.put("isStatic", isStatic);
    if (isNullOrEmpty(searchBy)) {
      Container<FolderInfo> repos = infoProvider.getRepos(sortBy, sortOrder);
      repos.setShowInfo(true);
      model.put("model", repos);
      return new ModelAndView("folderView", model);
    } else {
      model.put("model", infoProvider.find(searchBy, sortBy, sortOrder));
      model.put("searchBy", searchBy);
      return new ModelAndView("searchView", model);
    }
  }

  @RequestMapping(method = GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json")
  @ResponseBody
  public Container<FolderInfo> getRepositoriesAsJson(@RequestParam(
                                                                   value = "sortBy", required = false,
                                                                   defaultValue = "name"
                                                                  ) SortField sortBy,
                                                     @RequestParam(
                                                                   value = "order", required = false,
                                                                   defaultValue = "asc"
                                                                  ) SortOrder sortOrder) {
    return infoProvider.getRepos(sortBy, sortOrder);
  }

  @RequestMapping(value = "/{repoName}", method = GET, produces = TEXT_HTML_VALUE)
  public ModelAndView getArchsAsHtml(
    @PathVariable("repoName") String repoName,
    @RequestParam(value = "search", required = false) String searchBy,
    @RequestParam(value = "sortBy", required = false, defaultValue = "name") SortField sortBy,
    @RequestParam(value = "order", required = false, defaultValue = "asc") SortOrder sortOrder) {
    Map<String, Object> model = new HashMap<String, Object>();
    model.putAll(sorting(sortBy, sortOrder));
    model.put("isStatic", isStatic);
    if (isNullOrEmpty(searchBy)) {
      model.put("model", infoProvider.getArchs(repoName, sortBy, sortOrder));
      return new ModelAndView("folderView", model);
    } else {
      model.put("model", infoProvider.find(searchBy, repoName, sortBy, sortOrder));
      model.put("searchBy", searchBy);
      return new ModelAndView("searchView", model);
    }
  }

  protected abstract String getRepositoryInfoViewName();

  @RequestMapping(value = "/{repoName}/info.html", method = GET, produces = TEXT_HTML_VALUE)
  public ModelAndView getRepoInfo(@PathVariable("repoName") String repoName) {
    RepoEntry repoEntry = repoService.ensureEntry(repoName, infoProvider.getValidRepoTypes());
    return new ModelAndView(getRepositoryInfoViewName(), "repo", repoEntry);
  }

  @RequestMapping(value = "/{repoName}", method = GET, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Container<FolderInfo> getArchsAsJson(
    @PathVariable("repoName") String repoName,
    @RequestParam(value = "sortBy", required = false, defaultValue = "name") SortField sortBy,
    @RequestParam(value = "order", required = false, defaultValue = "asc") SortOrder sortOrder) {
    return infoProvider.getArchs(repoName, sortBy, sortOrder);
  }

  @RequestMapping(value = "/{repoName}/{arch}", method = GET, produces = TEXT_HTML_VALUE)
  public ModelAndView getFileInfoAsHtml(
    @PathVariable("repoName") String repoName,
    @PathVariable("arch") String arch,
    @RequestParam(value = "search", required = false) String searchBy,
    @RequestParam(value = "sortBy", required = false, defaultValue = "name") SortField sortBy,
    @RequestParam(value = "order", required = false, defaultValue = "asc") SortOrder sortOrder) {
    Map<String, Object> model = new HashMap<>();
    model.putAll(sorting(sortBy, sortOrder));
    model.put("isStatic", isStatic);
    if (isNullOrEmpty(searchBy)) {
      model.put("model", infoProvider.getFileInfo(repoName, arch, sortBy, sortOrder));
      return new ModelAndView("fileView", model);
    } else {
      model.put("model", infoProvider.find(searchBy, repoName, arch, sortBy, sortOrder));
      model.put("searchBy", searchBy);
      return new ModelAndView("searchView", model);
    }
  }

  @RequestMapping(value = "/{repoName}/{arch}", method = GET, produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Container<FileInfo> getFileInfoAsJson(
    @PathVariable("repoName") String repoName,
    @PathVariable("arch") String arch,
    @RequestParam(value = "sortBy", required = false, defaultValue = "name") SortField sortBy,
    @RequestParam(value = "order", required = false, defaultValue = "asc") SortOrder sortOrder) {
    return infoProvider.getFileInfo(repoName, arch, sortBy, sortOrder);
  }

  @RequestMapping(method = GET, produces = TEXT_PLAIN_VALUE)
  @ResponseBody
  public String getRepoListAsTextPlain(@RequestParam(value = "name", required = false, defaultValue = ".") String name,
                                       @RequestParam(value = "tag", required = false) Set<String> tags,
                                       @RequestParam(value = "notag", required = false) Set<String> notags,
                                       @RequestParam(value = "newer", required = false) Integer newerThanInDays,
                                       @RequestParam(value = "older", required = false) Integer olderThanInDays,
                                       @RequestParam(
                                                     value = "showDestination", required = false,
                                                     defaultValue = "false"
                                                    ) boolean showDestination) {
    final Set<RepoEntry> repoEntries;
    if (isStatic) {
      repoEntries = getStaticRepoEntriesForGivenParams(name, tags, notags, newerThanInDays, olderThanInDays);
    } else {
      repoEntries = getVirtualRepoEntriesForGivenParams(name);
    }
    if ((repoEntries != null) && (!repoEntries.isEmpty())) {
      return handleTargetAndJoinResultString(repoEntries, showDestination);
    } else {
      return "";
    }
  }

  private Set<RepoEntry> getStaticRepoEntriesForGivenParams(String name, Set<String> tags, Set<String> notags,
                                                            Integer newerThanInDays, Integer olderThanInDays) {
    Date newerDate = daysToDate(newerThanInDays, DEFAULT_OFFSET_STARTING_POINT);
    Date olderDate = daysToDate(olderThanInDays, DEFAULT_OFFSET_END_POINT);
    Set<RepoEntry> repoEntries = new HashSet<>();
    if (tags != null) {
      for (String tag : tags) {
        repoEntries.addAll(infoProvider.find(name, tag, newerDate, olderDate));
      }
    } else {
      repoEntries.addAll(infoProvider.find(name, newerDate, olderDate));
    }

    if (notags != null) {
      for (String tag : notags) {
        final List<RepoEntry> noTags = infoProvider.find(name, tag, newerDate, olderDate);
        repoEntries.removeAll(noTags);
      }
    }
    return repoEntries;
  }

  private Date daysToDate(Integer minusDaysFromNow, int defaultOffset) {
    final DateTime now = DateTime.now();
    if (minusDaysFromNow != null) {
      return now.minusDays(minusDaysFromNow).toDate();
    }
    return now.minusDays(defaultOffset).toDate();
  }

  private Set<RepoEntry> getVirtualRepoEntriesForGivenParams(String name) {
    Set<RepoEntry> repoEntries = new HashSet<>();
    final List<RepoEntry> repoEntriesList = infoProvider.find(name);

    repoEntries.addAll(repoEntriesList);
    return repoEntries;
  }


  private String handleTargetAndJoinResultString(Set<RepoEntry> repoEntries, boolean showDestination) {
    StringBuilder result = new StringBuilder();
    if (showDestination) {
      for (RepoEntry repoEntry : repoEntries) {
        result.append(repoEntry.getName() + " : " + repoEntry.getTarget() + "\n");
      }
      return result.toString();
    } else {
      return joinFrom(repoEntries, "\n").getName();
    }
  }

  protected Map<String, Object> sorting(SortField sortBy, SortOrder sortOrder) {
    HashMap<String, Object> sorting = newHashMap();
    for (SortField field : SortField.values()) {
      sorting.put("sortOrder" + capitalize(field.name()), asc);
      sorting.put("sortOrderDirection" + capitalize(field.name()), none);
    }
    sorting.put("sortOrder" + capitalize(sortBy.name()), sortOrder.reverse());
    sorting.put("sortOrderDirection" + capitalize(sortBy.name()), sortOrder);
    return sorting;
  }
}
