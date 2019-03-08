package adblocker;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

public class RequestHandler implements Runnable {

    private Socket client;

    public RequestHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            // our request stream
            BufferedReader clientRequest = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // our response stream
            DataOutputStream clientResponse = new DataOutputStream(client.getOutputStream());

            boolean isHead = false;

            // this is our header information
            // for example, GET / HTTP/1.1
            String headerBody = clientRequest.readLine();

            // find keys from header chunk
            int firstSpace = headerBody.indexOf(" ");
            int secondSpace = headerBody.indexOf(" ", firstSpace+1);

            String method = headerBody.substring(0, firstSpace);
            String route = headerBody.substring(firstSpace+1, secondSpace);
            String version = headerBody.substring(secondSpace+1);

            HttpRequest request = new HttpRequest(method, route, version);
            HttpResponse resp = new HttpResponse();

            //we handle our request
            switch (request.getHttpMethod()) {
                case "GET": resp = handleGET(request); break;
                case "HEAD": resp = handleGET(request); isHead = true; break;
                case "POST": resp = handlePOST(request); break;
                case "PUT": resp = handlePUT(request); break;
            }

            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();

            //we create and send a new header for the response
            clientResponse.writeBytes(resp.getProtocolVersion()+ " " + resp.getResponseCode() + " " + resp.getResponseMessage() +"\r\n");
            clientResponse.writeBytes("Date: " + dateFormat.format(date) + "\r\n");
            clientResponse.writeBytes("Content-Type: " + resp.getContentType() + "\r\n");

            // James F Kurose - Computer networks
            File file = null;
            int byteLength = 0;
            byte[] fileBytes = null;

            if (resp.getContentType() == "image/jpg") {
                file = new File("webpage/"+request.getRoute().substring(1));
                byteLength = (int) file.length();
                FileInputStream fileStream = new FileInputStream("webpage/"+request.getRoute().substring(1));
                fileBytes = new byte[byteLength];
                fileStream.read(fileBytes);
                clientResponse.writeBytes("Content-Length: " + byteLength + "\r\n");
                clientResponse.writeBytes("\r\n");
                if (!isHead)
                    clientResponse.write(fileBytes, 0, byteLength);
            }
            else {
                clientResponse.writeBytes("Content-Length: " + resp.getBody().length() + "\r\n");
                clientResponse.writeBytes("\r\n");
                if (!isHead)
                clientResponse.writeBytes(resp.getBody());
            }

            clientResponse.writeBytes("\n");

            clientResponse.close();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private HttpResponse handleGET(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String route = request.getRoute();

        if (!routeExists(route)) {
            response.setBody("");
            response.setResponseCode(404);
            response.setResponseMessage("Not found");
            response.setContentType("text/plain");
            return response;
        }

        try {
            if (request.getRoute().equals("/") || request.getRoute().endsWith(".html")) {
                BufferedReader in = new BufferedReader(new FileReader("webpage/index.html"));
                response.setBody(in.lines().collect(Collectors.joining()));
                response.setContentType("text/html");
                response.setResponseMessage("OK");
                response.setResponseCode(200);
            } else if (request.getRoute().endsWith(".jpg")) {
                response.setContentType("image/jpg");
                response.setResponseMessage("OK");
                response.setResponseCode(200);
            }
        } catch (FileNotFoundException e) {
            response.setResponseMessage("Not found");
            response.setResponseCode(404);
            response.setBody("");
        }

        return response;
    }

    private boolean routeExists(String route) {
        File directory = new File("webpage");
        File[] listOfFiles = directory.listFiles();

        //root
        if (route.equals("/")) return true;

        if (listOfFiles != null)
            for(File f : listOfFiles)
                if (f.isFile())
                    if (f.getName().equals(route.substring(1)))
                        return true;
        return false;
    }

    private HttpResponse handlePOST(HttpRequest request) {
        return null;
    }

    private HttpResponse handlePUT(HttpRequest request) {
        return null;
    }


}
