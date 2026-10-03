package games.cascadia.setup;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StartingHexLoader {

  public static StartingHexConfig load(String filePath) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      File file = new File(filePath);

      // Load from file system (like pandemic and other games do)
      if (!file.exists()) {
        throw new RuntimeException("starting_hexes.json not found at path: " + filePath);
      }

      return mapper.readValue(file, StartingHexConfig.class);

    } catch (Exception e) {
      e.printStackTrace(); // Print full stack trace for debugging
      throw new RuntimeException("Failed to load starting hex config from: " + filePath, e);
    }
  }
}