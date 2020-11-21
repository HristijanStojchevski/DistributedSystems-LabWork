import java.io.*;
import java.net.Socket;
import java.util.concurrent.Semaphore;

class Lock{

    private boolean isLocked = true;
    private Thread lockedBy = null;

    public synchronized void lock() throws InterruptedException{
//        lockedBy = Thread.currentThread();
        int i=0;
        while(isLocked){
            wait();
        }
        isLocked = true;
    }

    public synchronized void unlock(){
        isLocked = false;
        notify();
    }
}

class PrintSocket extends Thread{
   ObjectInputStream input;
   Lock lock;
   public PrintSocket(ObjectInputStream input,Lock lock){
       this.input = input;
       this.lock = lock;
   }

   public void run(){
       boolean firstOcurr = true;
       while (!interrupted()){
           try {
               Object obj = input.readObject();
               try {
                   Message tmp = (Message) obj;
                   System.out.println(tmp.toString());
                   if(tmp.getSender().equals("ServerConfirm")){
                       System.out.println("setting DONE to : " + tmp.getContent());
                       Client.done = tmp.getContent();
                       lock.unlock();
                   }
                   else if(!firstOcurr){
                       lock.unlock();
                   }
                   firstOcurr = false;
               }
               catch(ClassCastException e){
                   try {
                       ClientInfo c_info = (ClientInfo) obj;
                       System.out.println(c_info.toString());
                   } catch (Exception ex) {
                       ex.printStackTrace();
                       System.exit(1);
                   }
               }
           } catch (IOException | ClassNotFoundException e) {
               e.printStackTrace();
               System.err.println(e.getMessage());
           }
       }
       try {
           input.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
}

class UserSocket extends Thread{
    ObjectOutputStream out;
    BufferedReader in;
    Lock lock;
    public UserSocket(ObjectOutputStream writer, Lock lock) throws IOException {
        this.out = writer;
        this.in = new BufferedReader(new InputStreamReader(System.in));
        this.lock = lock;
    }

    public void run(){
        boolean end = false;
        synchronized (this) {
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Please enter your name");
        try {
                while (!Client.done.equals("done")) {
                    String name = in.readLine();
                    ClientInfo self = new ClientInfo(name, true, 50);
                    out.writeObject(self);
                    out.flush();

                    lock.lock();
                }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        while (!end){
            String tmp = null;
            try {
                tmp = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(tmp.equals("end")) end = true; //client disconnects and server is notified
            try {
                out.writeObject(new Message("Client","Server",tmp));
                out.flush();
                System.out.println("Com inside chat room");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}

public class Client {
    public static String done = "notDone";
    public static void main(String[] args) throws IOException {
        
        Socket s = new Socket("127.0.0.1", 80);
        OutputStream outputStream = s.getOutputStream();
        Lock lock = new Lock();
        ObjectOutputStream pw = new ObjectOutputStream(outputStream);
        ObjectInputStream i_stream = new ObjectInputStream(s.getInputStream());
        PrintSocket printSocket = new PrintSocket(i_stream,lock);
        printSocket.start();
        UserSocket userSocket = new UserSocket(pw,lock);
        userSocket.start();


    }
}
