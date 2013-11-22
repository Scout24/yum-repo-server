package de.is24.infrastructure.gridfs.http.monitoring;

public class MethodAnnotated {
  @TimeMeasurement
  public void methodOne() {
  }

  @TimeMeasurement
  public String methodTwo(String lala) {
    return lala + "blubb";
  }

}
