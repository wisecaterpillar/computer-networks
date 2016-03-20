package smtpserver;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class Message {
    private EMailAddress from;
    private EMailAddress to;
    private String contents = "";

    public boolean isForMyDomain (String domain)
    {
        return to.getDomain().equals(domain);
    }
    
    public boolean setFrom(String line) {
        from = new EMailAddress();
        return (from.setAddress(line));
    }

    public boolean setTo(String line) {
        to = new EMailAddress();
        return (to.setAddress(line));
    }
    
    public void addContents(String contents) {
        this.contents += contents + "\n";
    }

    public EMailAddress getTo() {
        return to;
    }
    
    public void save() {
        String timestamp = new SimpleDateFormat("HHmmssddMMyyyy").format((new GregorianCalendar()).getTime());
        FileWriter message;
        String path = to.getDomain() + "/MAIL/"+ to.getUsername();
        new File (path).mkdirs();
        try {
            message = new FileWriter(path
                    + "/"
                    + from.getUsername()
                    + "-"
                    + timestamp
                    + ".txt");
        } catch (IOException ex) {
            System.out.println("Cannot open file to save message");
            return;
        }
        try {
            message.write("From: "
                    + from.getFullAddress());
            message.write("\nTo: "
                    + to.getFullAddress());
            message.write("\nText:\n"
                    + contents);
        } catch (IOException ex) {
            System.out.println("Cannot write data to file");
        }
        try {
            message.close();
        } catch (IOException ex) {
            //Just deal with it OK?
        }
    }
}
