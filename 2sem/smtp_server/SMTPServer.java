package smtpserver;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SMTPServer extends Thread
{
    private FileWriter logger;
    private boolean running = true;
    private ServerSocket serverSocket;
    private Socket socket;
    private final ArrayList<Thread> clients;
    private int port = 25;
    private String host = "localhost";
    private final String domain = "test.ru";
    
    public SMTPServer()
    {
        this.clients = new ArrayList<Thread>();
    }

    public void stopWork()
    {
        running = false;
    }

    public void setRunning(boolean running) 
    {
        this.running = running;
    }
    
    public void setup()
    {
        boolean check = true;
        String cmd;
        Scanner in = new Scanner(System.in);
        System.out.println(">>Settings:<<");
        System.out.println("1) Port:\t " + port);
        System.out.println("2) Domain:\t " + domain);
        System.out.println("Type the number of the position you want to change, or q to quit");

        while (check)
        {
            System.out.print(">>");
            cmd = in.next();
            if (cmd.equals("1"))
            {
                System.out.println("Enter the port number:");
                this.port = in.nextInt();
            }

            else if (cmd.equals("2"))
            {
                System.out.println("Enter the domain name:");
                String line = in.next();
                if (line.split("\\.").length == 2)
                    this.host = line;
                else 
                    System.out.println("Invalid name");
            }
            else if (cmd.equals("q"))
                check = false;
            else
                System.out.println("Invalid command");
        }
        System.out.println(">>New Settings:<<");
        System.out.println("1) Port:\t " + port);
        System.out.println("2) Domain:\t " + host);
    }

    private void quit()
    {
        running = false;
        for (Thread client : clients) {
            client.stop();
        }
        
        try 
        {
            logger.close();
            if (!serverSocket.isClosed())
                serverSocket.close();
        
            if (socket.isConnected())
                socket.close();
        }
        catch (IOException ex) {System.out.println("Error while closing socket");
        }
        catch (NullPointerException ex){
        }
    }

    @Override
    public void run()             
    {
        String timestamp = new SimpleDateFormat("HHmmssddMM").format((new GregorianCalendar()).getTime());
        InetAddress listenAddress;
        
        try {
             listenAddress = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            System.out.println("Illegal hostname");
            return;
        }
        
        try 
        {
            serverSocket = new ServerSocket( port, 50, listenAddress );
        }
        catch (IOException ioe)
        {
            System.out.println("Cannot open socket on port "
                    + port);
            return;
        }
        
        (new File ("LOGs")).mkdirs();
        
        try {
            logger = new FileWriter("LOGs/"
                    + "log"
                    + timestamp
                    + ".txt");
        } catch (IOException ex) {
            System.out.println("Extremely critical error occured. We cannot create logging file. VERY SORRY!");
            return;
        }
        
        try {
            logger.write("New server socket on " + serverSocket.getLocalSocketAddress() + " opened.\n");
        } catch (IOException ex) {
            System.out.println("ERROR WRITING TO LOG!");
        }
        
        try {
            //Set the socket to timeout every 10 seconds so it does not
            //just block forever.
            serverSocket.setSoTimeout( 5 * 1000 );
        }
        catch( SocketException se ) {
            
        }
        
        while(running)
        {
            try {
                socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(logger,(clients.size()+1), socket, domain);
                Thread th = new Thread(handler);
                clients.add(th);
                th.start();
            }
            catch( InterruptedIOException iioe ) {
                //This is fine, it should time out every 10 seconds if
                //a connection is not made.
            }
            //If any exception gets to here uncaught, it means we should just disconnect.
            catch( IOException e ) {
//                System.out.println("Something is wrong :(");
                try {
                    if( socket != null ) {
                        socket.close();
                    }
                }
                catch( IOException ioe ) {         
                    //Just deal with it OK?
                }
            }
        }
        quit();
    }
}