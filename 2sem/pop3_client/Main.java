package pop3cli;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException 
    {
        String cmd;
        boolean run = true;
        Scanner in = new Scanner(System.in);
        
        POP3Cli client1 = new POP3Cli();
        JobSupplier jsup = new JobSupplier(client1);
        client1.setDebug(true);
        System.out.println("Type command. Use <h> or <help> for help.");
        printMenu();

        jsup.start();
        
        while (run)
        {
            System.out.print(">");
            cmd = in.next();
            
            if (cmd.equals("c"))
            {
                if (client1.connectAndLogin())
                        jsup.setRun(true);
                    else
                        System.out.println("Cannot connect to server for some reason. Sorry.");
            }
            else if (cmd.equals("h"))
                printMenu();
            else if (cmd.equals("st"))
                client1.getNumberAndSize();
            else if (cmd.equals("del"))
                client1.markMessage();
            else if (cmd.equals("q"))
            {
                client1.quit();
                    run = false;
                    jsup.setRun(false);
                    break;
            }

            else if (cmd.equals("l1"))
            {
                client1.loadOneMsg();
            }

            else if (cmd.equals("la"))
                client1.loadAll();
            else if (cmd.equals("ln"))
                    client1.loadnewmail();
            else if (cmd.equals("lh"))
                    client1.loadHeaders();
            else if (cmd.equals("lnh"))
                    client1.loadNewHeaders();
            else
                    System.out.println("Invalid command");
              
            }
        }
    
    
    private static void printMenu()
    {
        System.out.println("<h> or <help>\thelp\n"
                + "<c>\tConnect and login\n"
                + "<st>\tGet the number of new e-,mails and their average length\n"
                + "<lnh>\tGet all new e-mail headers\n"
                + "<lh>\tGet headers (addressee and subject) of e-mails in inbox\n"
                + "<ln>\tLoad all new mail\n"
                + "<l1>\tLoad definite e-mail (number required)\n"
                + "<la>\tLoad all e-mails from inbox\n"
                + "<del>\tMark a message to delete\n"
                + "<q>\tQuit the application\n");
    }
}
