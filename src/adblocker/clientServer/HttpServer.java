package adblocker.clientServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    /**
     * Create thread and handle each client socket connection separately
     */
    private void run() throws IOException {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(1024);

            // Handle each client socket connection in new Thread
            while(true) {
                Socket client = socket.accept();
                if (client != null) {
                    DataInputStream in = new DataInputStream(client.getInputStream());
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    Thread t = new Thread(new HttpRequestHandler(client, in, out));
                    t.start();
                }
            }
        } catch(IOException ex) {
            System.out.println(ex.getMessage());
            if (socket != null)
                socket.close();
        }
    }

    /**
     * Entry point for HTTP Server
     * @param args None
     */
    public static void main(String[] args) {
        HttpServer webServer = new HttpServer();
        try {
            webServer.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
