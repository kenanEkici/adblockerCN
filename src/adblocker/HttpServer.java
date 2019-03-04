package adblocker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer implements Runnable {



    @Override
    public void run() {
        try {
            ServerSocket socket = new ServerSocket(1024);

            while(true) {
                Socket client = socket.accept();
                if (client != null) {
                    ResponseHandler handler = new ResponseHandler(client);
                    Thread t = new Thread(handler);
                    t.start();
                }
            }
        } catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
