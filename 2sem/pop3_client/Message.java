package pop3cli;

public class Message 
{
    private final String addressee;
    
    private final String subject;

    private String body;

    protected Message(String addressee, String subject, String body) 
    {
        this.addressee = addressee;
        this.body = body;
        if(subject.equals(""))
            this.subject = "<no subject>";
        else 
            this.subject = subject;
    }
    
    protected Message(String addressee, String subject) 
    {
        this.addressee = addressee;
        this.body = "";
        if(subject.equals(""))
            this.subject = "<no subject>";
        else 
            this.subject = subject;
    }
    
    public void setBody(String line)
    {
        this.body = line;
    }
    
    public String getBody() 
    {
        return body;
    }
    
    public String getSub()
    {
        return subject;
    }
    
    public String getAddressee()
    {
        return addressee;
    }
}
