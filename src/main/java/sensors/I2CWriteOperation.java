package sensors;

/**
 * RPITank
 * Created by MAWood on 11/07/2016.
 */
public class I2CWriteOperation
{
    private final int address;
    private final int value;
    public I2CWriteOperation(int address, int value)
    {
        this.address = address;
        this.value = value;
    }

    public int getAddress()
    {
        return address;
    }

    public byte getEncodedValue()
    {
        return (byte) value;
    }
}
