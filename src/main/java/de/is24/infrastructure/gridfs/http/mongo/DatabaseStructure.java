package de.is24.infrastructure.gridfs.http.mongo;

public interface DatabaseStructure {
  String FILENAME_KEY = "filename";
  String ARCH_KEY = "arch";
  String REPO_KEY = "repo";
  String MARKED_AS_DELETED_KEY = "markedAsDeleted";
  String UPLOAD_DATE_KEY = "uploadDate";
  String ARCH_KEY_REPO_DATA = "repodata";
  String METADATA_ARCH_KEY = "metadata." + ARCH_KEY;
  String METADATA_REPO_KEY = "metadata." + REPO_KEY;
  String METADATA_UPLOAD_DATE_KEY = "metadata." + UPLOAD_DATE_KEY;
  String METADATA_MARKED_AS_DELETED_KEY = "metadata." + MARKED_AS_DELETED_KEY;
  String GRIDFS_FILES_COLLECTION = "fs.files";
  String YUM_ENTRY_COLLECTION = "yum.entries";
  String REPO_ENTRY_COLLECTION = "yum.repos";
}
