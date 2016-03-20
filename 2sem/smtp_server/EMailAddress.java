package smtpserver;

public class EMailAddress {
    private String username;
    private String domain;

    public EMailAddress() {
    }

    public boolean setAddress(String line) {
        String[] res = line.split("@");
        if (!isCorrect(line))
            return false;
        username = res[0];
        domain = res[1];
        return true;
    }
    
    public String getFullAddress () {
        return (username 
                + "@"
                + domain);
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public String getUsername() {
        return username;
    }
    
    public static boolean isCorrect(String address)
    {
        String[] res = address.split("@");
        if (res.length != 2)
            return false;
        String[] tdomain = res[1].split("\\.");
        if (tdomain.length != 2)
            return false;
        return true;
    }
}