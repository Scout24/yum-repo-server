package de.is24.infrastructure.gridfs.http.metadata.generation;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.jaxb.RepoMd;
import de.is24.util.monitoring.spring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.security.PGPSigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY_REPO_DATA;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static org.apache.commons.io.IOUtils.write;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@TimeMeasurement
public class RepoMdGenerator {
  public static final int INITIAL_BUFFER_SIZE = 4 * 1024;
  private final GridFS gridFs;
  private final JAXBContext jaxbContext;
  private final PGPSigner pgpSigner;

  /* for cglib */
  protected RepoMdGenerator() {
    jaxbContext = null;
    gridFs = null;
    pgpSigner = null;
  }

  @Autowired
  public RepoMdGenerator(GridFS gridFs, PGPSigner pgpSigner) {
    this.gridFs = gridFs;
    this.pgpSigner = pgpSigner;
    try {
      this.jaxbContext = JAXBContext.newInstance(RepoMd.class);
    } catch (JAXBException e) {
      throw new IllegalStateException("Could not initialize JAXBContext.", e);
    }
  }

  public void generateRepoMdXml(final String reponame, final List<Data> data) {
    byte[] content = createXml(createRepoMd(createRevision(), data));
    overrideFile(reponame, content, filename(reponame), APPLICATION_XML_VALUE);
    if (pgpSigner.isActive()) {
      byte[] signature = pgpSigner.sign(content);
      overrideFile(reponame, signature, signatureFilename(reponame), "application/x-gpg");
    }
  }

  private void overrideFile(String reponame, byte[] content, String filename, String contentType) {
    GridFSDBFile previousFile = gridFs.findOne(filename);

    GridFSInputFile file = gridFs.createFile();
    setAttributes(file, reponame, contentType, filename);

    writeContent(content, file.getOutputStream());

    removeIfExists(previousFile);
  }

  private void writeContent(byte[] content, OutputStream outputStream) {
    try {
      write(content, outputStream);
      outputStream.close();
    } catch (IOException e) {
      throw new IllegalStateException("Could not write repomd.xml", e);
    }
  }

  private void removeIfExists(GridFSDBFile gridFsFile) {
    if (null != gridFsFile) {
      gridFs.remove(gridFsFile);
    }
  }

  private void setAttributes(GridFSInputFile file, String reponame, String contentType, String filename) {
    DBObject metaData = createMetaData(reponame);
    file.setMetaData(metaData);
    file.setContentType(contentType);
    file.setFilename(filename);
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

  private DBObject createMetaData(String reponame) {
    DBObject metaData = new BasicDBObject();
    metaData.put(REPO_KEY, reponame);
    metaData.put(ARCH_KEY, ARCH_KEY_REPO_DATA);
    return metaData;
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
