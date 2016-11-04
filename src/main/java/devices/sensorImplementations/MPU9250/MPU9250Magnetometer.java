/**
 * 
 */
package devices.sensorImplementations.MPU9250;

import java.io.IOException;

import devices.sensors.Magnetometer;
import devices.sensors.dataTypes.TimestampedData3D;

/**
 * @author GJWood
 *
 */
public class MPU9250Magnetometer extends Magnetometer implements devices.sensors.interfaces.Magnetometer {

	/**
	 * @param sampleRate
	 * @param sampleSize
	 */
	public MPU9250Magnetometer(int sampleRate, int sampleSize) {
		super(sampleRate, sampleSize);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see devices.sensors.interfaces.Magnetometer#getLatestGaussianData()
	 */
	@Override
	public TimestampedData3D getLatestGaussianData() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see devices.sensors.interfaces.Magnetometer#getGaussianData(int)
	 */
	@Override
	public TimestampedData3D getGaussianData(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see devices.sensors.interfaces.Magnetometer#getMagnetometerReadingCount()
	 */
	@Override
	public int getMagnetometerReadingCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see devices.sensors.interfaces.Magnetometer#updateMagnetometerData()
	 */
	@Override
	public void updateMagnetometerData() throws Exception {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see devices.sensors.dataTypes.Sensor1D#updateData()
	 */
	@Override
	protected void updateData() throws IOException {
		// TODO Auto-generated method stub

	}

}
