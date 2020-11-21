import java.io.Serializable;

public class Message implements Serializable {
    private String sender;
    private String receiver;
    private String content;

    public Message() {
        this.sender = "";
        this.receiver = "";
        this.content = "";
    }

    public Message(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public Message(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        String str = "Sender: ";
        str += sender + "\n";
        str += "<--- Message ---> \n";
        str += content;
        return str;
    }
}
