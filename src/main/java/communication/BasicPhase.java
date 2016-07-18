package communication;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * RPITank - communication
 * Created by MAWood on 18/07/2016.
 */
public abstract class BasicPhase implements CommunicationPhase
{
    protected final DataInputStream in;
    protected final PrintStream out;
    protected boolean completed;

    BasicPhase(InputStream in, OutputStream out)
    {
        this.in = new DataInputStream(in);
        this.out = new PrintStream(out);
        completed = false;
    }

    @Override
    public boolean hasCompleted()
    {
        return completed;
    }
}
