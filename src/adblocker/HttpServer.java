package adblocker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    private void run() {
        try {
            ServerSocket socket = new ServerSocket(1024);

            while(true) {
                Socket client = socket.accept();
                if (client != null) {
                    HttpRequestHandler handler = new HttpRequestHandler(client);
                    Thread t = new Thread(handler);
                    t.start();
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
