package sensors;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Thermometer
{
    float getLatestTemperature();
    float getTemperature(int i);
}
