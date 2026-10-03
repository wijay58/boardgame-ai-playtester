package core.analytics.ger;

import java.util.HashMap;
import java.util.Map;

public class Resources {
  public double spendRate;
  public double usedRate;
  public double wasteRate;
  public double lateGameValueDecay;

  public Map<String, Resources> byType = new HashMap<>();
}
