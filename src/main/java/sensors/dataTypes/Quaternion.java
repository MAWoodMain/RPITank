package sensors.dataTypes;

public class Quaternion 
{
	public float a,b,c,d;
	
	public Quaternion()
	{
		this(0,0,0,0);
	}
	
	public Quaternion(float a,float b, float c, float d)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}
	public void normalize()
	{
		float norm;
		// Normalise accelerometer measurement
		norm = (float)Math.sqrt(a*a + b*b + c*c+ d*d);
		if (norm == 0.0f)
			throw new ArithmeticException(); // handle NaN
		norm = 1f / norm;
		a *= norm;
		b *= norm;
		c *= norm;
		d *= norm;
	}
}
