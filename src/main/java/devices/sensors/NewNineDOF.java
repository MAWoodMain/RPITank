package devices.sensors;

import devices.sensorImplementations.MPU9250.MPU9250Magnetometer;

public abstract class NewNineDOF extends SensorPackage {
	Magnetometer mag = new MPU9250Magnetometer(1,1);
	NewNineDOF(int sampleRate) {
		super(sampleRate);
		// TODO Auto-generated constructor stub
	}

	@Override
	void updateData() {
		// TODO Auto-generated method stub

	}

}
