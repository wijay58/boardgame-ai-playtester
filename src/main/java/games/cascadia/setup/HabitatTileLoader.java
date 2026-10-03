package games.cascadia.setup;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HabitatTileLoader {

  public static HabitatTileConfig load(String filePath) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      File file = new File(filePath);

      // Load from file system (like pandemic and other games do)
      if (!file.exists()) {
        throw new RuntimeException("habitat_tiles.json not found at path: " + filePath);
      }

      return mapper.readValue(file, HabitatTileConfig.class);

    } catch (Exception e) {
      e.printStackTrace(); // Print full stack trace for debugging
      throw new RuntimeException("Failed to load habitat tiles config from: " + filePath, e);
    }
  }
}