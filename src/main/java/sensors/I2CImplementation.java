package sensors;

import java.io.IOException;

/**
 * Created by MAWood on 17/07/2016.
 */
public interface I2CImplementation
{

    byte read(int address) throws IOException;
    byte[] read(int address, int count) throws IOException;

    void write(int address, byte data) throws IOException;

}
