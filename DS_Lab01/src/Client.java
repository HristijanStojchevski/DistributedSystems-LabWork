import java.io.*;
import java.net.Socket;

class PrintSocket extends Thread{
   BufferedReader reader;
   public PrintSocket(BufferedReader reader){
       this.reader = reader;
   }

   public void run(){
       while (!interrupted()){
           String tmp = null;
           try {
               tmp = reader.readLine();
           } catch (IOException e) {
               e.printStackTrace();
           }
           System.out.println(tmp);
       }
   }
}

class UserSocket extends Thread{
    PrintWriter writer;
    BufferedReader in;
    public UserSocket(PrintWriter writer){
        this.writer = writer;
        in = new BufferedReader(new InputStreamReader(System.in));
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String name = reader.readLine();
            writer.println(name);
            writer.flush();
        } catch (IOException e) {
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
            writer.println(tmp);
            writer.flush();
        }
        System.exit(0);
    }
}

public class Client {
    public static void main(String[] args) throws IOException {
        
        Socket s = new Socket("127.0.0.1", 80);
        OutputStream outputStream = s.getOutputStream();
        PrintWriter pw = new PrintWriter(outputStream, false);
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintSocket printSocket = new PrintSocket(br);
        printSocket.start();
        UserSocket userSocket = new UserSocket(pw);
        userSocket.start();


    }
}
