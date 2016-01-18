package com.mongodb.gridfs;

import com.mongodb.DBObject;

public final class GridFSUtil {
  private GridFSUtil() {
  }

  public static void remove(GridFSDBFile dbFile) {
    dbFile.remove();
  }

  public static void mergeMetaData(final GridFSFile gridFsFile, final DBObject metaDataToMerge) {
    final DBObject existingMetaData = gridFsFile.getMetaData();
    if (existingMetaData == null) {
      gridFsFile.setMetaData(metaDataToMerge);
    } else {
      existingMetaData.putAll(metaDataToMerge);
    }
  }
}
