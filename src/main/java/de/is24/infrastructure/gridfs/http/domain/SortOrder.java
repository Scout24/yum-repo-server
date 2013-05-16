package de.is24.infrastructure.gridfs.http.domain;

import org.springframework.data.domain.Sort.Direction;


public enum SortOrder {
  asc(1),
  desc(-1),
  none(0);

  public int value;

  private SortOrder(int value) {
    this.value = value;
  }

  public SortOrder reverse() {
    return (this == asc) ? desc : asc;
  }

  public Direction asDirection() {
    return (this == asc) ? Direction.ASC : Direction.DESC;
  }

}
