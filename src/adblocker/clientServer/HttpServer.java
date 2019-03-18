package adblocker.clientServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    /**
     * Create thread and handle each client socket connection separately
     */
    private void run() {
        try {
            ServerSocket socket = new ServerSocket(1024);

            // Handle each client socket connection in new Thread
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

    /**
     * Entry point for HTTP Server
     * @param args None
     */
    public static void main(String[] args) {
        HttpServer webServer = new HttpServer();
        webServer.run();
    }

}
