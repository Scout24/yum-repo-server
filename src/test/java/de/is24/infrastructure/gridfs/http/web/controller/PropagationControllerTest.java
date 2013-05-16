package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.web.controller.PropagationController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


public class PropagationControllerTest {

  private PropagationController controller;
  private GridFsService gridFs;

  @Before
  public void setUp() throws Exception {
    gridFs = mock(GridFsService.class);
    controller = new PropagationController(gridFs);
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void throwExceptionForFileNotFound() throws Exception {
    doThrow(GridFSFileNotFoundException.class).when(gridFs).propagateRpm(anyString(), anyString());

    controller.propgateRpm("not/existing/file", "dest-repo", new MockHttpServletResponse());
  }

  @Test
  public void moveFileToDestRepo() throws Exception {
    controller.propgateRpm("not/existing/file", "dest-repo", new MockHttpServletResponse());

    verify(gridFs).propagateRpm(anyString(), anyString());
  }

}
