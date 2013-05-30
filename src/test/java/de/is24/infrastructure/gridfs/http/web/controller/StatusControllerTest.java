package de.is24.infrastructure.gridfs.http.web.controller;

import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.Mongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.util.Set;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;


@RunWith(MockitoJUnitRunner.class)
public class StatusControllerTest {
  private static final String STATUS_OK_JSON = "{mongoDBStatus: \"ok\"}";
  private static final String STATUS_FAILURE_JSON = "{mongoDBStatus: \"not responding\"}";

  private StatusController statusController;
  @Mock
  private MongoTemplate mongoTemplate;
  @Mock
  private Mongo mongo;

  @Mock
  private CommandResult commandResult;

  private MockMvc mockMvc;
  private MockHttpServletRequestBuilder requestBuilder;

  @Before
  public void setUp() throws Exception {
    statusController = new StatusController(mongoTemplate, mongo);
    mockMvc = standaloneSetup(statusController).build();
    when(mongoTemplate.executeCommand(any(BasicDBObject.class))).thenReturn(commandResult);

    Set<String> set = Sets.newHashSet(
      "fs.chunks",
      "fs.files",
      "system.indexes",
      "system.users",
      "yum.entries",
      "yum.repos");
    when(mongoTemplate.getCollectionNames()).thenReturn(set);
  }

  @Test
  public void getOKJsonWhenDBIsAvailable() throws Exception {
    when(commandResult.ok()).thenReturn(true);

    requestBuilder = MockMvcRequestBuilders.get("/status");

    mockMvc.perform(requestBuilder)
    .andExpect(status().isOk())
    .andExpect(content().string(containsString(STATUS_OK_JSON)));

  }

  @Test
  public void getOKJsonWhenDBIsAvailableInPrettyJson() throws Exception {
    when(commandResult.ok()).thenReturn(true);

    requestBuilder = MockMvcRequestBuilders.get("/status-full");

    mockMvc.perform(requestBuilder)
    .andExpect(status().isOk())
    .andExpect(content().string(containsString("\"mongoDBStatus\" : \"ok\"")))
    .andExpect(content().string(not(containsString("}}"))));
  }

  @Test
  public void getNotRespondingJsonWhenDBIsNOAvailable() throws Exception {
    when(commandResult.ok()).thenReturn(false);

    requestBuilder = MockMvcRequestBuilders.get("/status");

    mockMvc.perform(requestBuilder)
    .andExpect(status().isServiceUnavailable())
    .andExpect(content().string(containsString(STATUS_FAILURE_JSON)));
  }
}
