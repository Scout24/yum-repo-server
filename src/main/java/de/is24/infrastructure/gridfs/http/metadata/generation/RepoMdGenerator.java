package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.jaxb.RepoMd;
import de.is24.infrastructure.gridfs.http.security.PGPSigner;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@TimeMeasurement
public class RepoMdGenerator {
  public static final int INITIAL_BUFFER_SIZE = 4 * 1024;
  private final FileStorageService fileStorageService;
  private final JAXBContext jaxbContext;
  private final PGPSigner pgpSigner;

  /* for cglib */
  protected RepoMdGenerator() {
    jaxbContext = null;
    fileStorageService = null;
    pgpSigner = null;
  }

  @Autowired
  public RepoMdGenerator(FileStorageService fileStorageService, PGPSigner pgpSigner) {
    this.fileStorageService = fileStorageService;
    this.pgpSigner = pgpSigner;
    try {
      this.jaxbContext = JAXBContext.newInstance(RepoMd.class);
    } catch (JAXBException e) {
      throw new IllegalStateException("Could not initialize JAXBContext.", e);
    }
  }

  public void generateRepoMdXml(final String reponame, final List<Data> data) {
    byte[] content = createXml(createRepoMd(createRevision(), data));
    fileStorageService.storeFile(new ByteArrayInputStream(content), new FileDescriptor(filename(reponame)), true);
    if (pgpSigner.isActive()) {
      byte[] signature = pgpSigner.sign(content);
      fileStorageService.storeFile(new ByteArrayInputStream(signature), new FileDescriptor(signatureFilename(reponame)), true);
    }
  }

  private byte[] createXml(RepoMd repoMd) {
    ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
    try {
      jaxbContext.createMarshaller().marshal(repoMd, tempOutputStream);
    } catch (JAXBException e) {
      throw new IllegalStateException("Unable to marshall RepoMd object.", e);
    }
    return tempOutputStream.toByteArray();
  }

  private String filename(String reponame) {
    return reponame + "/repodata/repomd.xml";
  }

  private String signatureFilename(String reponame) {
    return filename(reponame) + ".asc";
  }

  private RepoMd createRepoMd(long revision, List<Data> data) {
    RepoMd repoMd = new RepoMd();
    repoMd.setRevision(revision);
    repoMd.setData(data);
    return repoMd;
  }

  private long createRevision() {
    return System.currentTimeMillis();
  }
}
