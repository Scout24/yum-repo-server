package de.is24.infrastructure.gridfs.http.jaxb;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlType(propOrder = { "location", "checksum", "timestamp", "size", "openChecksum", "openSize", "databaseVersion" })
public class Data {
  private Location location;

  private Checksum checksum;

  private int timestamp;

  private long size;

  private long openSize;

  private Checksum openChecksum;

  private int databaseVersion;

  private String type;

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public Checksum getChecksum() {
    return checksum;
  }

  public void setChecksum(Checksum checksum) {
    this.checksum = checksum;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(int timestamp) {
    this.timestamp = timestamp;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  @XmlElement(name = "open-size")
  public long getOpenSize() {
    return openSize;
  }

  public void setOpenSize(long openSize) {
    this.openSize = openSize;
  }

  @XmlElement(name = "open-checksum")
  public Checksum getOpenChecksum() {
    return openChecksum;
  }

  public void setOpenChecksum(Checksum openChecksum) {
    this.openChecksum = openChecksum;
  }

  @XmlElement(name = "database_version")
  public int getDatabaseVersion() {
    return databaseVersion;
  }

  public void setDatabaseVersion(int databaseVersion) {
    this.databaseVersion = databaseVersion;
  }

  public void setChecksum(String type, String hash) {
    checksum = new Checksum();
    checksum.setType(type);
    checksum.setChecksum(hash);
  }

  public void setOpenChecksum(String type, String hash) {
    openChecksum = new Checksum();
    openChecksum.setType(type);
    openChecksum.setChecksum(hash);
  }

  public void setLocation(String locationStr) {
    location = new Location();
    location.setHref(locationStr);
  }

  @XmlAttribute
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (this.getClass() != o.getClass())) {
      return false;
    }

    Data other = (Data) o;
    return new EqualsBuilder().append(checksum, other.checksum)
      .append(databaseVersion, other.databaseVersion)
      .append(location, other.location)
      .append(openChecksum, other.openChecksum)
      .append(openSize, other.openSize)
      .append(size, other.size)
      .append(timestamp, other.timestamp)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(checksum)
      .append(databaseVersion)
      .append(location)
      .append(openChecksum)
      .append(openSize)
      .append(size)
      .append(timestamp)
      .toHashCode();
  }
}
