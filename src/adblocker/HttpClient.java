package adblocker;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HttpClient {

    public static void main(String[] args) {

        String httpCommand = args[0];
        String hostName = args[1];
        int portNumber = Integer.parseInt(args[2]);

        try {
            InetAddress addr = InetAddress.getByName(hostName);
            Socket socket = new Socket(addr, portNumber);

            boolean autoflush = true;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), autoflush);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(httpCommand + " / HTTP/1.1");
            out.println("Host: "+ hostName + ":" + Integer.toString(portNumber));
            out.println("Connection: Close");
            out.println();

            HttpResponse resp = new HttpResponse();
            String line = in.readLine();

            while(line != null && !line.equals(""))  {
                resp.appendToHeader(line);
                line = in.readLine();
            }

            String body = "";

            while(line != null) {
                body += line;
                line = in.readLine();
            }

            resp.setBody(body);
            Files.write(Paths.get("websiteOutputs/test.html"), resp.getBody().getBytes());

            socket.close();


        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
