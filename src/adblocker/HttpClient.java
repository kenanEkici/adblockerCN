package adblocker;

import java.io.*;
import java.net.Socket;
import java.util.stream.Collectors;

public class HttpClient {
    public static void main(String[] args) {
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try {

            Socket kkSocket = new Socket(hostName, portNumber);
            BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
            String result = in.lines().collect(Collectors.joining("\n"));
            System.out.println(result);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }



    }
}
