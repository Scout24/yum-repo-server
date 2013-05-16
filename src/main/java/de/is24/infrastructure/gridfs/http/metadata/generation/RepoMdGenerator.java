package de.is24.infrastructure.gridfs.http.metadata.generation;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.jaxb.RepoMd;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY_REPO_DATA;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@TimeMeasurement
public class RepoMdGenerator {
  private final GridFS gridFs;
  private final JAXBContext jaxbContext;

  @Autowired
  public RepoMdGenerator(GridFS gridFs) {
    this.gridFs = gridFs;
    try {
      this.jaxbContext = JAXBContext.newInstance(RepoMd.class);
    } catch (JAXBException e) {
      throw new IllegalStateException("Could not initialize JAXBContext.", e);
    }
  }

  public void generateRepoMdXml(final String reponame, final List<Data> data) {
    RepoMd repoMd = createRepoMd(createRevision(), data);

    GridFSDBFile previousRepoMdXmlFile = gridFs.findOne(createFilename(reponame));
    GridFSInputFile file = gridFs.createFile();
    setAttributes(reponame, file);
    saveXml(repoMd, file);

    if (null != previousRepoMdXmlFile) {
      gridFs.remove(previousRepoMdXmlFile);
    }
  }

  private void setAttributes(String reponame, GridFSInputFile file) {
    DBObject metaData = createMetaData(reponame);
    file.setMetaData(metaData);
    file.setContentType(APPLICATION_XML_VALUE);
    file.setFilename(createFilename(reponame));
  }

  private void saveXml(RepoMd repoMd, GridFSInputFile file) {
    try {
      jaxbContext.createMarshaller().marshal(repoMd, file.getOutputStream());
      file.getOutputStream().close();
    } catch (JAXBException e) {
      throw new IllegalStateException("Unable to marshall RepoMd object.", e);
    } catch (IOException e) {
      throw new IllegalStateException("Could not save repomd.xml to GridFS.", e);
    }
  }

  private DBObject createMetaData(String reponame) {
    DBObject metaData = new BasicDBObject();
    metaData.put(REPO_KEY, reponame);
    metaData.put(ARCH_KEY, ARCH_KEY_REPO_DATA);
    return metaData;
  }

  private String createFilename(String reponame) {
    return reponame + "/repodata/repomd.xml";
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
