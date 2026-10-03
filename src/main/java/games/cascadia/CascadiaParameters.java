package games.cascadia;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

/**
 * Parameters for the Cascadia game
 */
public class CascadiaParameters extends TunableParameters<CascadiaParameters> {

  private String dataPath;

  public CascadiaParameters() {
    _reset();
  }

  public CascadiaParameters(long seed) {
    setRandomSeed(seed);
    _reset();
  }

  public CascadiaParameters(String dataPath) {
    this.dataPath = dataPath;
    _reset();
  }

  public CascadiaParameters(String dataPath, long seed) {
    this.dataPath = dataPath;
    setRandomSeed(seed);
    _reset();
  }

  public String getDataPath() {
    return dataPath;
  }

  @Override
  public void _reset() {
    // Default parameter values
  }

  @Override
  protected AbstractParameters _copy() {
    CascadiaParameters params = new CascadiaParameters(dataPath);
    params.setRandomSeed(getRandomSeed());
    return params;
  }

  @Override
  public boolean _equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    // No additional fields to compare in CascadiaParameters yet
    return true;
  }

  @Override
  public CascadiaParameters instantiate() {
    return this;
  }
}
