package pop3cli;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;



public class POP3Cli 
{
    private Socket socket;
    private boolean debug = false;
    private BufferedReader reader;
    private FileWriter logger1;
    private PrintStream pwriter;
    private Scanner in;
    private String host;
    private String uname;
    private String upass;
    private int port;
    private boolean isMarked = false;


    private static final int DEFAULT_PORT = 110;
    

    public void setDebug(boolean debug) 
    {
        this.debug = debug;
    }
  
    public boolean connectAndLogin () 
    {
        if (isConnected())
        {
            System.out.println("You are already connected. Please, try to disconnect first.");
            return false;
        }
        String timestamp = new SimpleDateFormat("HHmmssddMMyyyy").format((new GregorianCalendar()).getTime());
        socket = new Socket();
        in = new Scanner(System.in);
        System.out.print("Type server name: ");
        host = in.next();
        System.out.print("Type port: ");
        try
        {
            port = in.nextInt();
        }
        catch (InputMismatchException ex)
        {
            System.out.println("Illegal port name.");
            return false;
        }
        
        (new File ("logs")).mkdirs();

        try {
            logger1 = new FileWriter("logs/log-"
                    + host
                    + "-"
                    + port
                    + "-"
                    + timestamp
                    + ".txt");
        } catch (IOException ex) {
            return false;
        }
        try
        {
            socket.connect(new InetSocketAddress(host, port));
        }
        catch (IOException ex)
        {
            System.out.println("Unable to onnect to specified port and server."
                    + "\nCheck Internet connection");
            return false;
        }
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pwriter = new PrintStream(new BufferedOutputStream(socket.getOutputStream(),2500),true);
            readResponseLine();
        }
        catch (IOException ioe)
        {
            
        }
        
                
        System.out.print("Type username: ");
        uname = in.next();
        System.out.print("Type password: ");
        upass = in.next();
        
        try
        {
            sendCommand("USER " + uname);
            sendCommand("PASS " + upass);
        }
        catch (RuntimeException re)
        {
            System.out.println("Illegal Username or Password");
            try {
                socket.close();
            } catch (IOException ex) {
                return false;
            }
            socket = null;
            return false;
        }
        catch (IOException e)
        {
            return false;
        }
        if (debug)
            try {
                logger1.write("\nConnected to the host "
                        + host
                        + " via port "
                        + port
                        + "\nUsername: "
                        + uname
                        + " Password: "
                        + upass);
        } catch (IOException ex) {
           
        }
        System.out.println("You are now connected to server! Hoorray!");
        return true;
    }
    
    public void markMessage ()
    {
        if (isConnected())
        {
            int nr;
            in.nextLine();
            
            System.out.println("Enter the number of message: ");

            try
            {
                nr = in.nextInt();
            }
            catch (InputMismatchException ime)
            {
                System.out.println("Enter correct number.");
                return;
            }
            try
            {
                sendCommand("DELE "+nr);
            }
            catch (RuntimeException ex)
            {
                System.out.println("No such message");
            }
            catch (IOException  ioe)
            {
            }
            isMarked = true;
        }
        else 
            System.out.println("You are not connected to any server.");
    }
    
    public void getNumberAndSize()
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        String response;
        try {
            response = sendCommand("STAT");
        } catch (IOException ex) {
            System.out.println("Error while executing this procedure.");
            return;
        }
        String[] values = response.split(" ");
        System.out.println("You have "
                + Integer.parseInt(values[1])
                + ". Their average length is "
                + Integer.parseInt(values[2])
                + ".");
    }
    
    public void loadHeaders()
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        try
        {           
            int nr = getNumberOfNewMessages();
            for (int i = 1; i < nr+1; i++)
            {
                sendCommand("TOP " + i + " " + 0);
                Message hdr = parseHeader();
                System.out.println(i
                        +". From: "
                        + hdr.getAddressee()
                        + " Subject: "
                        + hdr.getSub());
            }
        }
        catch  (IOException ioe)
        {
            System.out.println("Error");
        }
    }
    
    public void loadNewHeaders ()
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        int index = 0;
        String path = "old/"
                + uname
                + "/h/"; 
        (new File (path)).mkdirs();
        /**
         * 1. get number of new messages
         * 2. for each message get its UIDL
         * 3. Check if uidl is new to us
         * 4. if uidl is new - load the header
         * 
         */
        try
        {
            int num = getNumberOfNewMessages();
            for (int i = 1; i <= num; i++)
            {
                String uidl = getUIDL(i);
                if (!(new File (path + uidl)).exists())
                {
                    index++;
                    FileWriter fw = new FileWriter(path + uidl);
                    fw.close();
                    sendCommand("TOP " + i + " " + 0);
                    Message hdr = parseHeader();
                    System.out.println(i
                        +". From: "
                        + hdr.getAddressee()
                        + " Subject: "
                        + hdr.getSub());
                }
            }
            if (index == 0)
                System.out.println("No new mail");
        }
        catch (IOException ioe)
        {
            System.out.println("ERROR");
        }
    }
    
    public void loadOneMsg()
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        Message message;
        int nr;
        in.nextLine();
        System.out.println("Enter the number of message you want to load: ");
        try 
        {
            nr = in.nextInt();
        }
        catch (InputMismatchException ime)
        {
            System.out.println("Incorrect number");
            return;
        }
        try
        {
            try {
                message = getMessage(nr);
            } catch (IOException ex) {
                System.out.println("Cannot get message.");
                return;
            }
        }
        catch (RuntimeException re)
        {
            System.out.println("This message does not exist. Try again.");
            return;
        }
        System.out.println("From: "
                + message.getAddressee()
                + "\nSubject: "
                + message.getSub()
                + "\nText:\n"
                + message.getBody());
    }
    
    public void loadAll()
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        try{
            int nr = getNumberOfNewMessages();
            for (int i = 1; i < nr+1; i++) {

                Message mes = getMessage(i);
                System.out.println("----->" + i + "<-----");
                System.out.println("From: "
                        + mes.getAddressee()
                        + "\nSubject: "
                        + mes.getSub()
                        +"\nText:\n "
                        + mes.getBody());
            }
        }
        catch (IOException ioe)
        {
            System.out.println("error receiving headers");
        }
    }
    
    public void loadnewmail()
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        int index = 0;
        String path = "old/" + uname + "/m/";
        (new File (path)).mkdirs();
        try 
        {
            int number = getNumberOfNewMessages();
            for (int i = 1; i <= number; i++)
            {
                String uidl = getUIDL(i);
                if (!(new File (path  + uidl)).exists())
                {
                    index++;
                    FileWriter fw = new FileWriter(path + uidl);
                    fw.close();
                    Message mes = getMessage(i);
                    System.out.println("----->" + i + "<-----");
                    System.out.println("From: "
                        + mes.getAddressee()
                        + "\nSubject: "
                        + mes.getSub()
                        +"\nText:\n "
                        + mes.getBody());
                }
            }
            if (index == 0)
                System.out.println("No new mail");
                
        }
        catch (IOException ioe)
        {
            //no idea what happened if we got here....
            System.out.println("ERROR IN GETTING NEW MAIL");
        }
    }
    //quit
    public void quit() 
    {
        if (!isConnected())
        {
            System.out.println("You are not connected to server");
            return;
        }
        try
        {
        if(isMarked)
        {    
            System.out.println("Do you want to delete marked messages from "
                    + host
                    + "? (y/n)");
            String del = in.next();
            if (del.equalsIgnoreCase("n") || del.equalsIgnoreCase("no"))
                sendCommand("RSET");
        }
        logout();
        disconnect();
        }
        catch (IOException ioe)
        {
            
        }
        catch (NullPointerException exception)
        {

        }
    }
     
    public void noop () throws IOException
    {
        sendCommand("NOOP");
    }
         
    //----------------------------------
    
    protected String getUIDL(int i) throws IOException
    {
        String response = sendCommand("UIDL " + i);
        return response.split(" ")[2];
    }
    
    
    protected int getNumberOfNewMessages() throws IOException 
    {
        String response = sendCommand("STAT");
        String[] values = response.split(" ");
        return Integer.parseInt(values[1]);
    } 
    protected void disconnect() throws IOException 
    {
        if (!isConnected())
            throw new IllegalStateException("Not connected to a host");
        socket.close();
        reader.close();
        pwriter.close();
        reader = null;
        pwriter = null;
        if (debug)
            logger1.write("\nDisconnected from the host");
        logger1.close();
    }
    
    protected void login(String username, String password) throws IOException
    {
        sendCommand("USER " + username);
        sendCommand("PASS " + password);
    } 
    protected void logout() throws IOException 
    {
        sendCommand("QUIT");
    } 
    protected void connect(String host, int port) throws IOException 
    {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pwriter = new PrintStream(new BufferedOutputStream(socket.getOutputStream(),2500),true);
        if (debug)
            logger1.write("\nConnected to the host " 
                                + host 
                                + "via port " 
                                + port);
        readResponseLine();
    } 
    protected boolean isDebug() 
    {
        return debug;
    } 
    protected boolean isConnected()
    {
        return socket != null && socket.isConnected();
    }
    
    protected Message getMessage(int i) throws IOException 
    {
        String response;
        String  from = "",
                subject = "", 
                body = "";

        sendCommand("RETR " + i);
        
        
        String headerName;
        // process headers
        while ((response = readResponseLine()).length() != 0) 
        {
            if (response.startsWith("\t")) 
            {
                continue; //no process of multiline headers
            }
            
            int colonPosition = response.indexOf(":");
            try {
                headerName = response.substring(0, colonPosition);
            }
            catch (StringIndexOutOfBoundsException boundsException)
            {
                continue;
            }
            String headerValue = response.substring(colonPosition + 2);
            if (headerName.equalsIgnoreCase("from"))
                from = parseAddressee(headerValue);
            else if (headerName.equalsIgnoreCase("subject"))
                subject = headerValue;
        }
        // process body
        while (!(response = readResponseLine()).equals(".")) 
        {
            if (!response.startsWith("--") 
                    & !response.matches("^Content-.*:.*$") 
                    & !response.matches(".*</div>$") 
                    & !response.matches("^<div.*$")
                    & !response.matches("\n"))
                body += (response + "\n");
        }
        return new Message(from, subject, body);
    } 
    protected String readResponseLine() throws IOException
    {
        String response = reader.readLine();
        if (debug)
            logger1.write("\nDEBUG [in] : " + response);
        if (response.startsWith("-ERR"))
            throw new RuntimeException("Server has returned an error: " 
                                        + response.replaceFirst("-ERR ", ""));
        return response;
    }
    
    protected String sendCommand(String command) throws IOException 
    {
        if (debug) 
            logger1.write("\nDEBUG [out]: " + command);
        
        pwriter.println(command);
        return readResponseLine();
    }
    
    protected String parseAddressee(String line)
    {
        StringTokenizer st = new StringTokenizer(line, "<>");
        st.nextToken();
        return st.nextToken();
    }
    
    protected Message parseHeader() throws IOException
    {
        String response;
        String headerName ="";
        String from ="", subject="";
        while ((response = readResponseLine()).length() > 1) 
        {
            if (response.startsWith("\t")) 
            {
                continue; //no process of multiline headers
            }
            int colonPosition = response.indexOf(":");
            try {
                headerName = response.substring(0, colonPosition);
            }
            catch (StringIndexOutOfBoundsException sioobe)
            {
                continue;
            }
            catch (NullPointerException npe){
                continue;
            }
                
            String headerValue = response.substring(colonPosition + 2);
            if (headerName.equalsIgnoreCase("from"))
                from = parseAddressee(headerValue);
            else if (headerName.equalsIgnoreCase("subject"))
                subject = headerValue;
        }
        return new Message(from, subject);
    }
}

