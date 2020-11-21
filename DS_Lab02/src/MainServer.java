import com.sun.tools.javac.Main;

import javax.sound.midi.Soundbank;
import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class ConnectionHandler implements Runnable {
    private Socket cc;
    private String clientName;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ConnectionHandler(Socket cc) throws IOException {
        this.cc = cc;
        in = new ObjectInputStream(cc.getInputStream());
        out = new ObjectOutputStream(cc.getOutputStream());
        this.clientName = "";
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public void run() {
        try {
            out.writeObject(new Message("Server","","Welcome to the chat room"));
        while (true) {
                int cPort = cc.getPort();
                ClientInfo clientInfo = (ClientInfo) in.readObject();
            System.out.println("Client info read.... ; " + clientInfo.getName());
                synchronized (MainServer.clients) {
                    if (MainServer.clients.stream().anyMatch(client -> client.getName().equals(clientInfo.getName()))) {
                        System.out.println("Same name found - TRUE");
                        ClientInfo tmp = MainServer.clients.stream().filter(cInfo -> clientInfo.getName().equals(cInfo.getName())).findAny().orElse(null);
                        System.out.println("tmp populated");
                        if(tmp != null) {
                            System.out.println("tmp is not NULL");
                            if (isOnline(tmp)) {
                                out.writeObject(new Message("Server","","A user with this name already exists. Try a new one"));
                                out.flush();
                                continue;
                            } else {
                                MainServer.clients.remove(tmp);
                                System.out.println("old user removed");
                                MainServer.clients.add(new ClientInfo(clientInfo.getName(), true, cPort));
                                System.out.println("new active user added");
                                this.clientName = clientInfo.getName();
                                out.writeObject(new Message("ServerConfirm","","done"));
                                out.flush();
                            }
                        }
                    }
                    else {
                        MainServer.clients.add(new ClientInfo(clientInfo.getName(), true, cPort));
                        this.clientName = clientInfo.getName();
                        out.writeObject(new Message("ServerConfirm","","done"));
                        out.flush();
                    }
                }
                while (true) {
                    // listen to client
                    Message req = (Message) in.readObject();
                    System.out.println("This message is read:");
                    System.out.println(req.getContent());
                    System.out.println("Trying toString() of Message:");
                    System.out.println(req.toString());
//                        users.add(new ClientInfo("John", (Inet4Address) socket.getInetAddress()));
                    // Switch on a letter from keyboard
                    // Options: 1. List of users , 2. Send message to selected User, 3 = end . exit
                    switch (req.getContent()) {
                        case "1":
                            System.out.printf("List req");
                            //return list of users to client
                            out.writeObject(new Message("Server","","List of clients:"));
                            out.flush();
                            MainServer.clients.forEach(cInfo -> {
                                try {
                                    out.writeObject(cInfo);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            break;
                        case "2":
                            out.writeObject(new Message("Server",this.clientName,"Type the name of the user:"));
                            Message recepientName = (Message) in.readObject();
                            ClientInfo temp = MainServer.clients.stream().filter(c -> c.getName().equals(recepientName.getContent())).findFirst().orElse(null);
                            if(temp != null) {
                                if( temp.getPort() == cc.getPort()) {
                                    out.writeObject(new Message("Server",this.clientName,"There is no reason for you to write to yourself :D"));
                                    break;
                                }
                                if (isOnline(temp)) {
                                    out.writeObject(new Message("Server",this.clientName,"You have connected with ->" + recepientName.getContent()));
                                    int rPort = temp.getPort();
                                    Message msg;
                                    ObjectOutputStream cOut = MainServer.connections.stream().filter(t -> t.cc.getPort() == rPort).findFirst().get().out;
                                    while (!(msg = (Message) in.readObject()).getContent().equals("end!")) {
                                        System.out.println("Message from " + this.clientName + " to " + recepientName.getContent() + " is:");
                                        System.out.println(msg.getContent());
                                            sendMessage(new Message(this.clientName, recepientName.getContent(), msg.getContent()), cOut);
                                        }
                                } else out.writeObject(new Message("Server",this.clientName,"Sorry this user is offline now."));
                            }
                            else out.writeObject(new Message("Server",this.clientName, "There is no user with that name !"));
                            break;
                            //send message
                        case "end":
                            out.writeObject(new Message("Server",this.clientName,"Goodbye!"));
                            synchronized (MainServer.clients){
                            MainServer.clients.remove(MainServer.clients.stream().filter(c-> c.getPort() == cc.getPort()).findFirst().get());
                            MainServer.clients.add(new ClientInfo(clientInfo.getName(),false,cc.getPort()));
                            }
                            break;
                        default:
                            out.writeObject(new Message("Server",this.clientName,"You haven't selected a valid option"));
                    }

                }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isOnline(ClientInfo recipient) {
        return recipient.isStatus();
    }

    private void sendMessage(Message message, ObjectOutputStream cOut) throws IOException {
        cOut.writeObject(message);
        cOut.flush();
    }
}

public class MainServer {

    public static ArrayList<ConnectionHandler> connections = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(16);

    public static ArrayList<ClientInfo> clients = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        int port = 80;
        ServerSocket ss = new ServerSocket(port, 50);

        while (true){
            Socket conn = ss.accept();
            ConnectionHandler connThread = new ConnectionHandler(conn);
            connections.add(connThread);
            pool.execute(connThread);
        }
    }
}
