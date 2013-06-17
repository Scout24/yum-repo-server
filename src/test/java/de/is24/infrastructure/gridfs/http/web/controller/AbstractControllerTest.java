package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractControllerTest {

  @Mock
  protected GridFsService gridFs;
  
  protected MockMvc mockMvc;

  @Before
  public void setUp() throws Exception {
    mockMvc = standaloneSetup(new FileController(gridFs)).build();
  }
  
}
