import com.pi4j.io.serial.*;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
public class Main
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        final Serial serial = SerialFactory.createInstance();

        // create and register the serial data listener
        serial.addListener(new SerialDataEventListener() {
            @Override
            public void dataReceived(SerialDataEvent event) {
                // print out the data received to the console
                try
                {
                    System.out.print(event.getAsciiString());
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });

        try {
            // open the default serial port provided on the GPIO header
            serial.open("/dev/ttyprintk", 9600);

            // continuous loop to keep the program running until the user terminates the program
            while(true) {
                Thread.sleep(1000);
            }

        }
        catch(SerialPortException ex) {
            System.out.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
            return;
        }
    }
}
