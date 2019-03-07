package adblocker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    public void run() {
        try {
            ServerSocket socket = new ServerSocket(1024);

            while(true) {
                Socket client = socket.accept();
                if (client != null) {
                    RequestHandler handler = new RequestHandler(client);
                    Thread t = new Thread(handler);
                    t.run();
                }
            }
        } catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        HttpServer webServer = new HttpServer();
        webServer.run();
    }

}
