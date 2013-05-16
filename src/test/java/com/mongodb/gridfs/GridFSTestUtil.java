package com.mongodb.gridfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GridFSTestUtil {
  public static GridFSDBFile gridFSDBFile(final String content) throws IOException {
    GridFSDBFile gridFSDBFile = mock(GridFSDBFile.class);
    when(gridFSDBFile.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(gridFSDBFile.getLength()).thenReturn((long) content.length());
    when(gridFSDBFile.getChunk(eq(0))).thenReturn(content.getBytes());
    when(gridFSDBFile.getContentType()).thenReturn("application/x-rpm");
    return gridFSDBFile;
  }
}
