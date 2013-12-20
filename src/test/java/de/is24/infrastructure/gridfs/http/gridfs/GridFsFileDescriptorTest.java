package de.is24.infrastructure.gridfs.http.gridfs;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class GridFsFileDescriptorTest {
  @Test
  public void combineRepoArchAnfFilename() throws Exception {
    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor("repo", "arch", "filename");
    String path = gridFsFileDescriptor.getPath();

    assertThat(path, is("repo/arch/filename"));

  }
}
