package sensors;

public class SensorFusion {

	static float[] eInt = new float[3];
	static float Ki = 0;
	static float Kp = 0;
	float deltat;
	float beta;
	Quaternion q;

	// Sensors x (y)-axis of the accelerometer is aligned with the y (x)-axis of
	// the magnetometer;
	// the magnetometer z-axis (+ down) is opposite to z-axis (+ up) of
	// accelerometer and g.getY()ro!
	// We have to make some allowance for this orientationmismatch in feeding
	// the output to the quaternion filter.
	// For the MPU-9250, we have chosen a magnetic rotation that keeps the
	// sensor forward along the x-axis just like
	// in the LSM9DS0 sensor. This rotation can be modified to allow any
	// convenient orientation convention.
	// This is ok by aircraft orientation standards!
	// Pass g.getY()ro rate as rad/s

	// MadgwickQuaternionUpdate(ax, ay, az, g.getX()*PI/180.0f, g.getY()*PI/180.0f,
	// g.getZ()*PI/180.0f,m.getY(), m.getX(),m.getZ());
	// MahonyQuaternionUpdate(ax, ay, az, g.getX()*PI/180.0f, g.getY()*PI/180.0f,
	// g.getZ()*PI/180.0f,m.getY(), m.getX(),m.getZ());

	// Implementation of Sebastian Madgwick's
	// "...efficient orientation filter for... inertial/magnetic sensor arrays"
	// (see http://www.x-io.co.uk/category/open-source/ for examples and more
	// details)
	// which fuses acceleration, rotation rate, and magnetic moments to produce
	// a quaternion-based estimate of absolute
	// device orientation -- which can be converted to yaw, pitch, and roll.
	// Useful for stabilizing quadcopters, etc.
	// The performance of the orientation filter is at least as good as
	// conventional Kalman-based filtering algorithms
	// but is much less computationally intensive---it can be performed on a 3.3
	// V Pro Mini operating at 8 MHz!

	void MadgwickQuaternionUpdate(Data3D a, Data3D g, Data3D m)

	{
		float q1 = q.a, q2 = q.b, q3 = q.c, q4 = q.d; // short name local
														// variable for
														// readability
		float norm;
		float hx, hy, _2bx, _2bz;
		float s1, s2, s3, s4;
		float qDot1, qDot2, qDot3, qDot4;

		// Auxiliary variables to avoid repeated arithmetic
		float _2q1mx;
		float _2q1my;
		float _2q1mz;
		float _2q2mx;
		float _4bx;
		float _4bz;
		float _2q1 = 2.0f * q1;
		float _2q2 = 2.0f * q2;
		float _2q3 = 2.0f * q3;
		float _2q4 = 2.0f * q4;
		float _2q1q3 = 2.0f * q1 * q3;
		float _2q3q4 = 2.0f * q3 * q4;
		float q1q1 = q1 * q1;
		float q1q2 = q1 * q2;
		float q1q3 = q1 * q3;
		float q1q4 = q1 * q4;
		float q2q2 = q2 * q2;
		float q2q3 = q2 * q3;
		float q2q4 = q2 * q4;
		float q3q3 = q3 * q3;
		float q3q4 = q3 * q4;
		float q4q4 = q4 * q4;

		
		a.normalize(); // Normalise accelerometer measurement
		m.normalize(); // Normalise magnetometer measurement


		// Reference direction of Earth's magnetic field
		_2q1mx = 2.0f * q1 * m.getX();
		_2q1my = 2.0f * q1 *m.getY();
		_2q1mz = 2.0f * q1 *m.getZ();
		_2q2mx = 2.0f * q2 * m.getX();
		hx = m.getX() * q1q1 - _2q1my * q4 + _2q1mz * q3 + m.getX() * q2q2 + _2q2 *m.getY() * q3
				+ _2q2 *m.getZ() * q4 - m.getX() * q3q3 - m.getX() * q4q4;
		hy = _2q1mx * q4 +m.getY() * q1q1 - _2q1mz * q2 + _2q2mx * q3 -m.getY() * q2q2
				+m.getY() * q3q3 + _2q3 *m.getZ() * q4 -m.getY() * q4q4;
		_2bx = (float) Math.sqrt(hx * hx + hy * hy);
		_2bz = -_2q1mx * q3 + _2q1my * q2 +m.getZ() * q1q1 + _2q2mx * q4 -m.getZ() * q2q2
				+ _2q3 *m.getY() * q4 -m.getZ() * q3q3 +m.getZ() * q4q4;
		_4bx = 2.0f * _2bx;
		_4bz = 2.0f * _2bz;

		// Gradient decent algorithm corrective step
		s1 = -_2q3 * (2.0f * q2q4 - _2q1q3 - a.getX()) + _2q2
				* (2.0f * q1q2 + _2q3q4 - a.getY()) - _2bz * q3
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - m.getX())
				+ (-_2bx * q4 + _2bz * q2)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -m.getY()) + _2bx
				* q3
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -m.getZ());
		s2 = _2q4 * (2.0f * q2q4 - _2q1q3 - a.getX()) + _2q1
				* (2.0f * q1q2 + _2q3q4 - a.getY()) - 4.0f * q2
				* (1.0f - 2.0f * q2q2 - 2.0f * q3q3 - a.getZ()) + _2bz * q4
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - m.getX())
				+ (_2bx * q3 + _2bz * q1)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -m.getY())
				+ (_2bx * q4 - _4bz * q2)
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -m.getZ());
		s3 = -_2q1 * (2.0f * q2q4 - _2q1q3 - a.getX()) + _2q4
				* (2.0f * q1q2 + _2q3q4 - a.getY()) - 4.0f * q3
				* (1.0f - 2.0f * q2q2 - 2.0f * q3q3 - a.getZ())
				+ (-_4bx * q3 - _2bz * q1)
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - m.getX())
				+ (_2bx * q2 + _2bz * q4)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -m.getY())
				+ (_2bx * q1 - _4bz * q3)
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -m.getZ());
		s4 = _2q2 * (2.0f * q2q4 - _2q1q3 - a.getX()) + _2q3
				* (2.0f * q1q2 + _2q3q4 - a.getY()) + (-_4bx * q4 + _2bz * q2)
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - m.getX())
				+ (-_2bx * q1 + _2bz * q3)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -m.getY()) + _2bx
				* q2
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -m.getZ());
		
		norm = (float) Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4); // normalise step magnitude
		norm = 1.0f / norm;
		s1 *= norm;
		s2 *= norm;
		s3 *= norm;
		s4 *= norm;

		// Compute rate of change of quaternion
		qDot1 = 0.5f * (-q2 * g.getX() - q3 * g.getY() - q4 * g.getZ()) - beta * s1;
		qDot2 = 0.5f * (q1 * g.getX() + q3 * g.getZ() - q4 * g.getY()) - beta * s2;
		qDot3 = 0.5f * (q1 * g.getY() - q2 * g.getZ() + q4 * g.getX()) - beta * s3;
		qDot4 = 0.5f * (q1 * g.getZ() + q2 * g.getY() - q3 * g.getX()) - beta * s4;

		// Integrate to yield quaternion
		q1 += qDot1 * deltat;
		q2 += qDot2 * deltat;
		q3 += qDot3 * deltat;
		q4 += qDot4 * deltat;
		norm = (float) Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4); // normalise
																			// quaternion
		norm = 1.0f / norm;
		q.a = q1 * norm;
		q.b = q2 * norm;
		q.c = q3 * norm;
		q.d = q4 * norm;

	}

	// Similar to Madgwick scheme but uses proportional and integral filtering
	// on the error between estimated reference vectors and
	// measured ones.
	void MahonyQuaternionUpdate(Data3D a, Data3D g, Data3D m)

	{
		float q1 = q.a, q2 = q.b, q3 = q.c, q4 = q.d; // short name local
														// variable for
														// readability
		float norm;
		float hx, hy, bx, bz;
		float vx, vy, vz, wx, wy, wz;
		float ex, ey, ez;
		float pa, pb, pc;

		// Auxiliary variables to avoid repeated arithmetic
		float q1q1 = q1 * q1;
		float q1q2 = q1 * q2;
		float q1q3 = q1 * q3;
		float q1q4 = q1 * q4;
		float q2q2 = q2 * q2;
		float q2q3 = q2 * q3;
		float q2q4 = q2 * q4;
		float q3q3 = q3 * q3;
		float q3q4 = q3 * q4;
		float q4q4 = q4 * q4;

		
		a.normalize();// Normalise accelerometer measurement
		m.normalize();// Normalise magnetometer measurement
		
		// Reference direction of Earth's magnetic field
		hx = 2.0f * m.getX() * (0.5f - q3q3 - q4q4) + 2.0f *m.getY() * (q2q3 - q1q4)
				+ 2.0f *m.getZ() * (q2q4 + q1q3);
		hy = 2.0f * m.getX() * (q2q3 + q1q4) + 2.0f *m.getY() * (0.5f - q2q2 - q4q4)
				+ 2.0f *m.getZ() * (q3q4 - q1q2);
		bx = (float) Math.sqrt((hx * hx) + (hy * hy));
		bz = 2.0f * m.getX() * (q2q4 - q1q3) + 2.0f *m.getY() * (q3q4 + q1q2) + 2.0f *m.getZ()
				* (0.5f - q2q2 - q3q3);

		// Estimated direction of gravity and magnetic field
		vx = 2.0f * (q2q4 - q1q3);
		vy = 2.0f * (q1q2 + q3q4);
		vz = q1q1 - q2q2 - q3q3 + q4q4;
		wx = 2.0f * bx * (0.5f - q3q3 - q4q4) + 2.0f * bz * (q2q4 - q1q3);
		wy = 2.0f * bx * (q2q3 - q1q4) + 2.0f * bz * (q1q2 + q3q4);
		wz = 2.0f * bx * (q1q3 + q2q4) + 2.0f * bz * (0.5f - q2q2 - q3q3);

		// Error is cross product between estimated direction and measured
		// direction of gravity
		ex = (a.getY() * vz - a.getZ() * vy) + (m.getY() * wz -m.getZ() * wy);
		ey = (a.getZ() * vx - a.getX() * vz) + (m.getZ() * wx -m.getX() * wz);
		ez = (a.getX() * vy - a.getY() * vx) + (m.getX() * wy -m.getY() * wx);
		if (Ki > 0.0f) {
			eInt[0] += ex; // accumulate integral error
			eInt[1] += ey;
			eInt[2] += ez;
		} else {
			eInt[0] = 0.0f; // prevent integral wind up
			eInt[1] = 0.0f;
			eInt[2] = 0.0f;
		}

		// Apply feedback terms
		g.setX( g.getX() + Kp * ex + Ki * eInt[0]);
		g.setY( g.getY() + Kp * ey + Ki * eInt[1]);
		g.setZ( g.getZ() + Kp * ez + Ki * eInt[2]);

		// Integrate rate of change of quaternion
		pa = q2;
		pb = q3;
		pc = q4;
		q1 = q1 + (-q2 * g.getX() - q3 * g.getY() - q4 * g.getZ()) * (0.5f * deltat);
		q2 = pa + (q1 * g.getX() + pb * g.getZ() - pc * g.getY()) * (0.5f * deltat);
		q3 = pb + (q1 * g.getY() - pa * g.getZ() + pc * g.getX()) * (0.5f * deltat);
		q4 = pc + (q1 * g.getZ() + pa * g.getY() - pb * g.getX()) * (0.5f * deltat);

		// Normalise quaternion
		norm = (float) Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);
		norm = 1.0f / norm;
		q.a = q1 * norm;
		q.b = q2 * norm;
		q.c = q3 * norm;
		q.d = q4 * norm;

	}
}
