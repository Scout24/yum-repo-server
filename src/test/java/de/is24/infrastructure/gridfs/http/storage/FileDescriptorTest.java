package de.is24.infrastructure.gridfs.http.storage;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class FileDescriptorTest {
  @Test
  public void combineRepoArchAndFilename() throws Exception {
    FileDescriptor fileDescriptor = new FileDescriptor("repo", "arch", "filename");
    String path = fileDescriptor.getPath();

    assertThat(path, is("repo/arch/filename"));

  }
}
