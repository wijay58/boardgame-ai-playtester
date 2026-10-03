package core.analytics.ger;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GERExporter {

  public static void export(GER ger, String fileName) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(new File(fileName), ger);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
