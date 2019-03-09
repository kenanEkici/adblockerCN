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
            PrintWriter out = new PrintWriter(client.getOutputStream());
            BufferedOutputStream byteOut= new BufferedOutputStream(client.getOutputStream());

            // this is our header information
            // for example, GET / HTTP/1.1
            String headerBody = inStream.readLine();
            HttpRequest request = parseIncomingHeader(headerBody);

            //we handle our request by method
            switch (request.getHttpMethod()) {
                case "GET": handleGET(out, byteOut, request, false); break;
                case "HEAD": handleGET(out, byteOut, request, true); break;
                case "POST": handlePOST(out, request); break;
                case "PUT": handlePUT(out, request); break;
                default: //TODO
            }

            out.close();
            byteOut.close();
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

    private void handleOutGoingHeader(PrintWriter out, HttpResponse resp) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        //we create and send a new header for the response
        out.println(resp.getProtocolVersion()+ " " + resp.getResponseCode() + " " + resp.getResponseMessage());
        out.println("Date: " + dateFormat.format(date));
        out.println("Content-Type: " + resp.getContentType());
        if (!resp.getTransferEncoding().equals("none")) out.println("Transfer-Encoding: " + resp.getTransferEncoding());
        else out.println("Content-Length: " + resp.getContentLength());
        out.println(); //EOH
        out.flush();
    }

    private void handleGET(PrintWriter out, BufferedOutputStream byteOut, HttpRequest request, boolean isHead) throws IOException {
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

                if (!isHead) {
                    out.println(response.getBody());
                    out.flush();
                }
            } else {
                response.setContentType("image/jpg");
                response.setResponseMessage("OK");
                response.setResponseCode(200);

                File file = new File("webpage/" + request.getRoute().substring(1));
                int length = (int) file.length();
                byte[] content = readFileData(file, length);
                response.setContentLength(length);
                handleOutGoingHeader(out, response);

                if (!isHead) {
                    byteOut.write(content, 0, length);
                    byteOut.flush();
                }
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

    private void handlePOST(PrintWriter out, HttpRequest request) {

    }

    private void handlePUT(PrintWriter out, HttpRequest request) {

    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }


}
