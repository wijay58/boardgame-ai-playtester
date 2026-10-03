package core.analytics.ger;

public class DesignAnnotation {
  public String severity; // INFO, WARNING, CRITICAL
  public String title;
  public String explanation;
  public String suggestion;

  public DesignAnnotation(String severity, String title, String explanation, String suggestion) {
    this.severity = severity;
    this.title = title;
    this.explanation = explanation;
    this.suggestion = suggestion;
  }
}
