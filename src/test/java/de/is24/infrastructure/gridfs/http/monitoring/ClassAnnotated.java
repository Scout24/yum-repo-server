package de.is24.infrastructure.gridfs.http.monitoring;

@TimeMeasurement
public class ClassAnnotated {
  public void methodOne() {
  }

  public String methodTwo(String lala) {
    return lala + "blubb";
  }
}
