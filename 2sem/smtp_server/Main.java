package smtpserver;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException 
    {
        SMTPServer server = new SMTPServer();
        boolean run = true;
        boolean isRunning = false;
        Scanner in = new Scanner(System.in);
        String cmd;
        help();
        System.out.print(">");
        
        while (run)
        {
            
            cmd = in.next();
            if(cmd.equals("set"))
            {
                if(!isRunning)
                    server.setup();
                else
                    System.out.println("You cannot set up server while it is running.\n" +
                            "Currently, the only ability to edit settings is to restart.");
            }
            else if(cmd.equalsIgnoreCase("q"))
            {
                run = false;
                server.stopWork();
            }
            else if(cmd.equalsIgnoreCase("h") || cmd.equalsIgnoreCase("help"))
            {
               help();
            }
            else if(cmd.equalsIgnoreCase("s"))
            {
                if (!isRunning)
                {
                    server.start();
                    isRunning = true;
                }
                else
                    System.out.println("It is already on");
            }
            else 
                System.out.println("Invalid command");
            
            System.out.print(">");
        }
        System.out.println("Good bye!");
    }
    
    private static void help()
    {
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println("~Available commands for my one and only server~");
        System.out.println("<h>\t\tHelp\n"
                + "<s>\t\tStart\n"
                + "<set>\t\tSetup (NOTE that you cannot change setting after the server is turned on)\n"
                + "<q>\t\tQuit");
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }
}