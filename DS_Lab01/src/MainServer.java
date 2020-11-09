import com.sun.tools.javac.Main;

import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class ClientInfo {
    private String name;
    private boolean status;
    private int port;

    public ClientInfo(String name,boolean status,int port) {
        this.name = name;
        this.status = status;
        this.port = port;
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

class Message {
    private String sender;
    private String receiver;
    private String content;

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
}

class ConnectionHandler implements Runnable {
    private Socket cc;
    private String clientName;
    private BufferedReader in;
    private PrintWriter out;

    public ConnectionHandler(Socket cc) throws IOException {
        this.cc = cc;
        in = new BufferedReader(new InputStreamReader(cc.getInputStream()));
        out = new PrintWriter(cc.getOutputStream(), true);
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public void run() {
        out.println("Welcome to the chat room");
        try {
        while (true) {
                int cPort = cc.getPort();
                String name = in.readLine();
                synchronized (MainServer.clients) {
                    if (MainServer.clients.stream().anyMatch(client -> client.getName().equals(name))) {
                        System.out.println("Same name found - TRUE");
                        ClientInfo tmp = MainServer.clients.stream().filter(clientInfo -> name.equals(clientInfo.getName())).findAny().orElse(null);
                        System.out.println("tmp populated");
                        if(tmp != null) {
                            System.out.println("tmp is not NULL");
                            if (isOnline(tmp)) {
                                out.println("A user with this name already exists. Try a new one");
                                continue;
                            } else {
                                MainServer.clients.remove(tmp);
                                System.out.println("old user removed");
                                MainServer.clients.add(new ClientInfo(name, true, cPort));
                                System.out.println("new active user added");
                            }
                        }
                    }
                    else MainServer.clients.add(new ClientInfo(name, true, cPort));
                }
                while (true) {
                    // listen to client
                    String req = in.readLine();
                    System.out.println(req);
//                        users.add(new ClientInfo("John", (Inet4Address) socket.getInetAddress()));
                    // Switch on a letter from keyboard
                    // Options: 1. List of users , 2. Send message to selected User, 3 = end . exit
                    switch (req) {
                        case "1":
                            System.out.printf("List req");
                            //return list of users to client
                            out.println("List of clients:");
                            MainServer.clients.forEach(clientInfo -> {
                                String status = "";
                                if (clientInfo.isStatus()) {
                                    status = "ACTIVE";
                                } else status = "OFFLINE";
                                out.println(" - Name: " + clientInfo.getName() + " , status: " + status);
                            });
                            break;
                        case "2":
                            out.println("Type the name of the user:");
                            String recepientName = in.readLine();
                            ClientInfo temp = MainServer.clients.stream().filter(c -> c.getName().equals(recepientName)).findFirst().orElse(null);
                            if(temp != null) {
                                if( temp.getPort() == cc.getPort()) {
                                    out.println("There is no reason for you to write to yourself :D");
                                    break;
                                }
                                if (isOnline(temp)) {
                                    int rPort = temp.getPort();
                                    String msg;
                                    PrintWriter cOut = MainServer.connections.stream().filter(t -> t.cc.getPort() == rPort).findFirst().get().out;
                                    while (!(msg = in.readLine()).equals("end!")) {
                                        sendMessage(new Message(name, recepientName, msg), cOut);
                                    }
                                } else out.println("Sorry this user is offline now.");
                            }
                            else out.println("There is no user with that name !");
                            break;
                            //send message
                        case "end":
                            out.println("Goodbye!");
                            synchronized (MainServer.clients){
                            MainServer.clients.remove(MainServer.clients.stream().filter(c-> c.getPort() == cc.getPort()).findFirst().get());
                            MainServer.clients.add(new ClientInfo(name,false,cc.getPort()));
                            }
                            break;
                        default:
                            out.println("You haven't selected a valid option");
                    }

                }
        }
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            out.close();
        }
    }

    private boolean isOnline(ClientInfo recipient) {
        return recipient.isStatus();
    }

    private void sendMessage(Message message, PrintWriter cOut) {
        cOut.println("New message from " + message.getSender() + ":");
        cOut.println(message.getContent());
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
