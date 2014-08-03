package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.jaxb.RepoMd;
import de.is24.infrastructure.gridfs.http.security.PGPSigner;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepoMdGeneratorTest {
  private FileStorageService fileStorageService;
  private PGPSigner pgpSigner;
  private RepoMdGenerator repoMdGenerator;

  @Before
  public void setup() {
    fileStorageService = mock(FileStorageService.class);
    pgpSigner = mock(PGPSigner.class);
    when(pgpSigner.isActive()).thenReturn(true);
    repoMdGenerator = new RepoMdGenerator(fileStorageService, pgpSigner);
  }

  @Test
  public void correctFilenameIsSet() throws Exception {
    String reponame = "any-reponame";

    repoMdGenerator.generateRepoMdXml(reponame, new ArrayList<Data>());

    verify(fileStorageService).storeFile(any(InputStream.class), eq(new GridFsFileDescriptor(createFilename(reponame))));
  }

  @Test
  public void repoMdXmlMatchesExpectedResult() throws Exception {
    Path path = Files.createTempFile(null, null);
    final FileOutputStream os = new FileOutputStream(path.toFile());
    Data data = createData();

    when(pgpSigner.isActive()).thenReturn(false);
    when(fileStorageService.storeFile(any(InputStream.class), any(GridFsFileDescriptor.class), eq(true))).then(new Answer<FileStorageItem>() {
      @Override
      public FileStorageItem answer(InvocationOnMock invocation) throws Throwable {
        InputStream inputStream = (InputStream) invocation.getArguments()[0];
        IOUtils.copy(inputStream, os);
        return null;
      }
    });

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
}
