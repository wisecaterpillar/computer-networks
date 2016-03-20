package pop3cli;

import java.io.IOException;

public class JobSupplier extends Thread
{
    private final POP3Cli cli;
    private boolean run;

    public JobSupplier(POP3Cli cli) {
        this.cli = cli;
        this.run = false; 
    }
    
    public void setRun(boolean value)
    {
        this.run = value;
    }
    
    @Override
    public void run()
    {
        while (!run)
        {
            
        }
        while(run)
        {
            try{
                cli.noop();
            }
            catch (IOException ioe)
            {
            }
            catch (NullPointerException npe)
            {
            }
            try 
            {
                Thread.sleep(60000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
