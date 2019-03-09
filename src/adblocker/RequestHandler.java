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
            BufferedReader inStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // our response stream
            DataOutputStream outStream = new DataOutputStream(client.getOutputStream());

            // this is our header information
            // for example, GET / HTTP/1.1
            String headerBody = inStream.readLine();
            HttpRequest request = parseIncomingHeader(headerBody);

            //we handle our request by method
            switch (request.getHttpMethod()) {
                case "GET": handleGET(outStream, request, false); break;
                case "HEAD": handleGET(outStream, request, true); break;
                case "POST": handlePOST(outStream, request); break;
                case "PUT": handlePUT(outStream, request); break;
                default: //TODO
            }
            outStream.writeBytes("\r\n"); //EOR

            outStream.close();
            inStream.close();
            client.close();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private HttpRequest parseIncomingHeader(String header) {

        // find keys from header chunk
        int firstSpace = header.indexOf(" ");
        int secondSpace = header.indexOf(" ", firstSpace+1);

        String method = header.substring(0, firstSpace);
        String route = header.substring(firstSpace+1, secondSpace);
        String version = header.substring(secondSpace+1);

        return new HttpRequest(method, route, version);
    }

    private void handleOutGoingHeader(DataOutputStream out, HttpResponse resp) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        //we create and send a new header for the response
        out.writeBytes(resp.getProtocolVersion()+ " " + resp.getResponseCode() + " " + resp.getResponseMessage() +"\r\n");
        out.writeBytes("Date: " + dateFormat.format(date) + "\r\n");
        out.writeBytes("Content-Type: " + resp.getContentType() + "\r\n");
        if (!resp.getTransferEncoding().equals("none")) out.writeBytes("Transfer-Encoding: " + resp.getTransferEncoding() + "\r\n");
        else out.writeBytes("Content-Length: " + resp.getContentLength() + "\r\n");
        out.writeBytes("\r\n"); //EOH
    }

    private void handleGET(DataOutputStream out, HttpRequest request, boolean isHead) throws IOException {
        HttpResponse response = new HttpResponse();
        String route = request.getRoute();

        if (!routeExists(route)) {
            response.setBody("");
            response.setResponseCode(404);
            response.setResponseMessage("Not found");
            response.setContentType("text/plain");
            handleOutGoingHeader(out, response);
        }

        try {
            // root
            if (request.getRoute().equals("/") || request.getRoute().endsWith(".html")) {
                BufferedReader in = new BufferedReader(new FileReader("webpage/index.html"));
                response.setBody(in.lines().collect(Collectors.joining()));
                response.setContentType("text/html");
                response.setResponseMessage("OK");
                response.setResponseCode(200);
                response.setContentLength(response.getBody().getBytes().length);
                handleOutGoingHeader(out, response);

                if (!isHead)
                    out.writeBytes(response.getBody());
            } else {
                response.setContentType("image/jpg");
                response.setResponseMessage("OK");
                response.setResponseCode(200);
                File file = null;
                int byteLength = 0;
                byte[] fileBytes = null;

                file = new File("webpage/" + request.getRoute().substring(1));
                byteLength = (int) file.length();
                response.setContentLength(byteLength);
                handleOutGoingHeader(out, response);


                FileInputStream fileStream = new FileInputStream("webpage/" + request.getRoute().substring(1));
                fileBytes = new byte[byteLength];
                fileStream.read(fileBytes);
                if (!isHead)
                    out.write(fileBytes, 0, byteLength);
            }
        } catch (FileNotFoundException e) {
            response.setResponseMessage("Not found");
            response.setResponseCode(404);
            response.setBody("");
            response.setContentLength(0);
            handleOutGoingHeader(out, response);
        }
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

    private void handlePOST(DataOutputStream out, HttpRequest request) {

    }

    private void handlePUT(DataOutputStream out, HttpRequest request) {

    }


}
