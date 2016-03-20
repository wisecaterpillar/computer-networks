package smtpserver;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable
{    
    private final FileWriter logger;
    private final Socket socket;
    private Message message = null;
    private PrintWriter out;
    private BufferedReader in;
    private final int id;
    private boolean running = true;
    private final String domain;

    public ClientHandler(FileWriter logger, int id, Socket socket, String domain)
    {
        this.id = id;
        this.socket = socket;
        this.logger = logger;
        this.domain = domain;
    }
    
    @Override
    public void run() 
    {        
        try {            
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader( socket.getInputStream() ));
        } catch (IOException ex) {
            System.out.println("ERROR_"
                    + id
                    + ":\tFilewriter, PrintWriter and BufferedReader are NOT initialized. Cannot establish further work.");
            return;
        }
        try {
            logger.write("[INFO_"
                    + id
                    + "]:\tNew connection from "
                    + socket.getLocalSocketAddress()
                    + "\n");
        } catch (IOException ex) {
            System.out.println("I can do here whatever I want, bc nothing can bring us here. Amen.");
        }
        try {
            socket.setSoTimeout( 100 * 1000 );
        } catch (SocketException ex) {
            write(MESSAGE_DISCONNECT);
        }
         try {
            socket.setSoTimeout( 100 * 1000 );
        } catch (SocketException ex) {
            write(MESSAGE_DISCONNECT);
        }
        write( WELCOME_MESSAGE );
        handleCommands();
        try {
            if( socket != null ) 
                socket.close();
            in.close();
            out.close();
        } catch( IOException ioe ) {
                    System.out.println("ERROR_"
                            + id
                            + ":\tCannot close socket.");
        }
    }    
    /** 
     * Parses the input for commands and delegates to the appropriate methods.
     */
    private void handleCommands() 
    {
        //Reusable Variables.
        String inputString = null;
        int lastCommand = NONE;
        
        while( running ) {
            try 
            {
              inputString = readLine();
            }   
            catch (RuntimeException re)
            {
            }
            if( inputString.startsWith("HELO") ) 
            {  
                write( "250 Hello " + inputString );
                lastCommand = HELO;
            }
            else if( inputString.toUpperCase().startsWith( "MAIL FROM:" ) ) {
                if( lastCommand == HELO || lastCommand == NONE || lastCommand == DATA) 
                {
                    message = new Message();
                    if (!isCorrect(inputString))
                        write(MESSAGE_MISSING_ARGUMENTS);
                    else if (!message.setFrom(handleCMD(inputString)))
                    {
                        write(MESSAGE_INVALID_ARGUMENT);
                    }
                    else
                    {
                        lastCommand = MAIL_FROM;
                        write(MESSAGE_OK);
                    }
                }
                else 
                {
                    write( MESSAGE_COMMAND_ORDER_INVALID );
                }
            }
            else if(  inputString.toUpperCase().startsWith( "RCPT TO:" ) ) 
            {
                if (lastCommand == RCPT_TO || lastCommand == MAIL_FROM)
                {
                    if (!isCorrect(inputString))
                        write(MESSAGE_MISSING_ARGUMENTS);
                    else
                    {
                        if (!message.setTo(handleCMD(inputString)))
                        {
                            write(MESSAGE_INVALID_ARGUMENT);
                        }
                        else
                        {
                            write (MESSAGE_OK);
                            lastCommand = RCPT_TO;
                        }
                    }
                }
                else {
                    write( MESSAGE_COMMAND_ORDER_INVALID );
                }
            }
            else if( inputString.toUpperCase().equals("DATA") ) 
            {
                if( lastCommand == RCPT_TO) 
                {
                    write( MESSAGE_SEND_DATA );
                    handleData();
                    lastCommand = DATA;
                    if (isToMyDomain())
                        message.save();
                    else 
                    {
                        resend();
                    }
                    write(MESSAGE_OK);
                }
                else {
                    write( MESSAGE_COMMAND_ORDER_INVALID );
                }
            }
            
            else if( inputString.toUpperCase().startsWith("QUIT") ) 
            {
                running = false;
            }
            else
                write (MESSAGE_INVALID_COMMAND);
        }
    }
    
    private String handleCMD (String s) 
    {
        String[] result = s.split(":"); 
        return result[1].replaceAll(" <","").replaceAll("<","").replaceAll(">","");
    }
    
    private boolean isCorrect (String s) 
    {
        String[] result = s.split(":"); 
        if (result.length!=2)
            return false;
        return result[1].matches("^[ ]*<.+>$");
    }
    
    private void handleData ()
    {
        String inputString = readLine();
        while( !inputString.equals( "." ) ) 
        {
            message.addContents(inputString);
            inputString = readLine();
        }
    }
        
    private void write( String message ) 
    {
        out.print( message + "\r\n" );
        try {
            logger.write("[OUT_"
                    + id
                    + "]:\t"
                    + message
                    + "\n");
        } catch (IOException ex) {
            System.out.println("ERROR WRITING TO LOG!");
        }
        out.flush();
    }
    
    private String readLine() 
    {
        try {
            String inputLine = in.readLine().trim();
            logger.write("[IN_"
                    + id
                    + "]:\t\t"
                    + inputLine
                    + "\n");
            return inputLine;
        }
        catch( IOException ioe ) {
            try {
                logger.write("[INFO_"
                        + id
                        + "]:\tError reading from socket"
                        + "\n");
            } catch (IOException ex) {
                System.out.println("ERROR WRITING TO LOG!");
            }
            throw new RuntimeException();
        }
    }
    
    private void resend ()
    {
        try{
            logger.write("\nResending this message to the server: "
                    + message.getTo().getDomain()
                    + "\n");
        }
        catch (IOException ioe)
        {
            System.out.println("ERROR WRITING TO LOG! Do you have any idea what hinders the bad dancer?...");
        }
                
                    
        System.out.println("LAME!");
    }
    
    private boolean isToMyDomain ()
    {
        return message.getTo().getDomain().equals(domain);
    }
    
    private static final String WELCOME_MESSAGE = "220 Welcome to my SMTPish server";
    private static final String MESSAGE_DISCONNECT = "221 SMTP server signing off";
    private static final String MESSAGE_OK = "250 OK";
    private static final String MESSAGE_COMMAND_ORDER_INVALID = "503 Command not allowed here";    
    private static final String MESSAGE_SEND_DATA = "354 Start mail input; end with <CRLF>.<CRLF>";
    private static final String MESSAGE_INVALID_COMMAND = "500 Command Unrecognized: ";
    private static final String MESSAGE_USER_NOT_LOCAL = "550 User does not exist.";
    private static final String MESSAGE_MISSING_ARGUMENTS = "550 MISSING ARGUMENTS.";
    private static final String MESSAGE_INVALID_ARGUMENT = "501 Your argument is invalid.";
    
    //SMTP Commands
    public int NONE = 0;
    public int HELO = 1;
    public int QUIT = 2;
    public int MAIL_FROM = 3;
    public int RCPT_TO = 4;
    public int DATA = 5;
}