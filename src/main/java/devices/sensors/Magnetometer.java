package devices.sensors;

import devices.sensors.dataTypes.Sensor3D;

/**
 * RPITank - devices.sensors
 * Created by MAWood on 18/07/2016.
 */
public abstract class Magnetometer extends Sensor3D
{
    public Magnetometer(int sampleRate, int sampleSize)
    {
        super(sampleRate, sampleSize);
    }
}
