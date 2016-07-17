package inertialNavigation;

public class Quaternion 
{
	public float a,b,c,d;
	
	public Quaternion(float a,float b, float c, float d)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}
	public Quaternion()
	{
		this(0,0,0,0);
	}
	public Quaternion(float[] data)
	{
		this.a = data[0];
		this.b = data[0];
		this.c = data[0];
		this.d = data[0];
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
	public void setAll(float a,float b, float c, float d)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

}
