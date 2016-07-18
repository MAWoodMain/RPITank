package communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * RPITank - communication
 * Created by MAWood on 18/07/2016.
 */
public class Session implements Runnable
{
    private final ArrayList<CommunicationPhase> communicationPhases;
    private final Socket socket;
    private final InputStream  input;
    private final OutputStream output;

    Session(Socket socket) throws IOException
    {
        this.socket = socket;
        this.communicationPhases = new ArrayList<>();
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }


    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            for(CommunicationPhase communicationPhase:communicationPhases)
            {
                while(!communicationPhase.hasCompleted())
                {
                    communicationPhase.run();
                }
            }
            Thread.currentThread().interrupt();
        }
    }
}
