package communication;

/**
 * RPITank - communication
 * Created by MAWood on 18/07/2016.
 */
public interface CommunicationPhase
{
    void begin();
    void run();
    boolean hasCompleted();

}
