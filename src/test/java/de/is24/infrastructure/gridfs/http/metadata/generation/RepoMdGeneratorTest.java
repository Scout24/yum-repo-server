package de.is24.infrastructure.gridfs.http.metadata.generation;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.jaxb.RepoMd;
import de.is24.infrastructure.gridfs.http.metadata.generation.RepoMdGenerator;
import org.apache.commons.io.output.NullOutputStream;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY_REPO_DATA;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepoMdGeneratorTest {
  private GridFS gridFs;
  private GridFSInputFile gridFsinputFile;
  private RepoMdGenerator repoMdGenerator;

  @Before
  public void setup() {
    gridFs = mock(GridFS.class);
    gridFsinputFile = mock(GridFSInputFile.class);
    repoMdGenerator = new RepoMdGenerator(gridFs);
  }

  @Test
  public void correctMetaDataIsCreated() throws Exception {
    String reponame = "any-reponame";
    BasicDBObject metaData = createExpectedMetaData(reponame);

    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(new NullOutputStream());

    repoMdGenerator.generateRepoMdXml(reponame, new ArrayList<Data>());

    verify(gridFsinputFile).setMetaData(metaData);
  }

  @Test
  public void correctFilenameIsSet() throws Exception {
    String reponame = "any-reponame";

    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(new NullOutputStream());

    repoMdGenerator.generateRepoMdXml(reponame, new ArrayList<Data>());

    verify(gridFsinputFile).setFilename(createFilename(reponame));
  }

  @Test
  public void correctContentTypeIsSet() throws Exception {
    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(new NullOutputStream());

    repoMdGenerator.generateRepoMdXml("any-reponame", new ArrayList<Data>());

    verify(gridFsinputFile).setContentType(MediaType.APPLICATION_XML.toString());
  }

  @Test
  public void gridFsFileIsActuallySaved() throws IOException {
    OutputStream outputStream = mock(OutputStream.class);

    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(outputStream);

    repoMdGenerator.generateRepoMdXml("any-reponame", new ArrayList<Data>());

    verify(outputStream).close();
  }

  @Test
  public void previousRepoMdXmlIsDeletedIfExists() throws IOException {
    String reponame = "any-reponame";
    GridFSDBFile gridFSDBFile = mock(GridFSDBFile.class);

    when(gridFs.findOne(createFilename(reponame))).thenReturn(gridFSDBFile);
    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(new NullOutputStream());

    repoMdGenerator.generateRepoMdXml(reponame, new ArrayList<Data>());

    verify(gridFs).remove(gridFSDBFile);
  }

  @Test
  public void previousRepoMdXmlIsNOTDeletedIfNotExists() throws IOException {
    String reponame = "any-reponame";

    when(gridFs.findOne(createFilename(reponame))).thenReturn(null);
    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(new NullOutputStream());

    repoMdGenerator.generateRepoMdXml(reponame, new ArrayList<Data>());

    verify(gridFs, never()).remove(any(String.class));
    verify(gridFs, never()).remove(any(BasicDBObject.class));
    verify(gridFs, never()).remove(any(ObjectId.class));
  }

  @Test
  public void repoMdXmlMatchesExpectedResult() throws Exception {
    Path path = Files.createTempFile(null, null);
    FileOutputStream os = new FileOutputStream(path.toFile());
    Data data = createData();

    when(gridFs.createFile()).thenReturn(gridFsinputFile);
    when(gridFsinputFile.getOutputStream()).thenReturn(os);

    repoMdGenerator.generateRepoMdXml("any-reponame", asList(data));

    RepoMd repoMd = readXmlFromTempFile(path);
    assertThat(repoMd.getRevision(), Matchers.greaterThan(1L));
    assertThat(repoMd.getData().size(), equalTo(1));
    assertThat(repoMd.getData().get(0), equalTo(data));
  }

  private String createFilename(String reponame) {
    return reponame + "/repodata/" + "repomd.xml";
  }

  private Data createData() {
    Data data = new Data();
    data.setChecksum("checksum", "hash");
    data.setDatabaseVersion(10);
    data.setLocation("location");
    data.setOpenChecksum("type", "hash");
    data.setOpenSize(1L);
    data.setSize(2L);
    data.setTimestamp(3);
    return data;
  }

  private RepoMd readXmlFromTempFile(Path path) throws JAXBException, FileNotFoundException {
    JAXBContext jaxbContext = JAXBContext.newInstance(RepoMd.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    FileInputStream is = new FileInputStream(path.toFile());
    return (RepoMd) unmarshaller.unmarshal(is);
  }

  private BasicDBObject createExpectedMetaData(String reponame) {
    BasicDBObject metaData = new BasicDBObject();
    metaData.put(REPO_KEY, reponame);
    metaData.put(ARCH_KEY, ARCH_KEY_REPO_DATA);
    return metaData;
  }
}
