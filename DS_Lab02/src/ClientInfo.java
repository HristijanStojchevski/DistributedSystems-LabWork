import java.io.Serializable;

public class ClientInfo implements Serializable {
    private String name;
    private boolean status;
    private int port;

    public ClientInfo() {
    }

    public ClientInfo(String name, boolean status, int port) {
        this.name = name;
        this.status = status;
        this.port = port;
    }

    @Override
    public String toString() {
        String status = "";
        if (this.isStatus()) {
            status = "ACTIVE";
        } else status = "OFFLINE";
        return " - Name: " + this.name + " , status: " + status;
    }

    public int getPort() {
        return port;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
