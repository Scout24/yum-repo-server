package de.is24.infrastructure.gridfs.http.web.controller;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import de.is24.infrastructure.gridfs.http.web.controller.StatusController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class StatusControllerTest {
  private static final String STATUS_OK_JSON = "{mongoDBStatus: 'ok'}";
  private static final String STATUS_FAILURE_JSON = "{mongoDBStatus: 'not responding'}";

  private StatusController statusController;
  @Mock
  private MongoTemplate mongoTemplate;

  @Mock
  private CommandResult commandResult;

  private MockHttpServletResponse mockResponse;

  @Before
  public void setUp() throws Exception {
    statusController = new StatusController(mongoTemplate);
    mockResponse = new MockHttpServletResponse();
    when(mongoTemplate.executeCommand(any(BasicDBObject.class))).thenReturn(commandResult);
  }

  @Test
  public void getOKJsonWhenDBIsAvailable() {
    when(commandResult.ok()).thenReturn(true);
    assertThat(statusController.getStatus(mockResponse), is(STATUS_OK_JSON));
    assertThat(mockResponse.getStatus(), is(SC_OK));

  }

  @Test
  public void getNotRespondingJsonWhenDBIsNOAvailable() {
    when(commandResult.ok()).thenReturn(false);
    assertThat(statusController.getStatus(mockResponse), is(STATUS_FAILURE_JSON));
    assertThat(mockResponse.getStatus(), is(SC_SERVICE_UNAVAILABLE));
  }

}
