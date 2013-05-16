package de.is24.infrastructure.gridfs.http.jaxb;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


@XmlRootElement(name = "repomd")
@XmlType(propOrder = { "revision", "data" })
public class RepoMd {
  private long revision;

  private List<Data> data;

  public long getRevision() {
    return revision;
  }

  public void setRevision(long revision) {
    this.revision = revision;
  }

  public List<Data> getData() {
    return data;
  }

  public void setData(List<Data> data) {
    this.data = data;
  }
}
