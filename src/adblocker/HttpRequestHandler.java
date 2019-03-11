package adblocker;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

public class HttpRequestHandler implements Runnable {

    private Socket client;

    public HttpRequestHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        PrintWriter out = null;
        try {
            // our request stream
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // our response streams
            out = new PrintWriter(client.getOutputStream());
            BufferedOutputStream byteOut= new BufferedOutputStream(client.getOutputStream());

            // read our header
            // for example, GET / HTTP/1.1
            HttpRequest request = new HttpRequest();
            String line = in.readLine();
            while (line != null && !line.equals("")) {
                request.appendToHeader(line);
                line = in.readLine();
            }

            //throws 400 if header does not contain Host for 1.1
            if (request.getProtocolVersion().equals("HTTP/1.1") && request.getHeader().get("Host") == null) {
                HttpResponse resp = new HttpResponse();
                throwBadRequest(out, resp);
            } else {
                //otherwise we handle our request by method
                switch (request.getHttpMethod()) {
                    case "GET": handleGET(out, byteOut, request, false); break;
                    case "HEAD": handleGET(out, byteOut, request, true); break;
                    case "POST": handlePOST(out, request); break;
                    case "PUT": handlePUT(out, request); break;
                    default: //TODO
                }
            }

            out.close();
            byteOut.close();
            in.close();
            client.close();

        } catch (IOException ex) {
            throwServerErrorRequest(out, new HttpResponse());
            System.out.println(ex.getMessage());
        }
    }

    private void handleOutGoingHeader(PrintWriter out, HttpResponse resp) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        //we create and send a new header for the response
        out.println(resp.getProtocolVersion()+ " " + resp.getResponseCode() + " " + resp.getResponseMessage());
        out.println("Date: " + dateFormat.format(date));
        if (resp.getContentType()!= null) out.println("Content-Type: " + resp.getContentType());
        if (!resp.getTransferEncoding().equals("none")) out.println("Transfer-Encoding: " + resp.getTransferEncoding());
        else out.println("Content-Length: " + resp.getContentLength());
        out.println(); //EOH
        out.flush();
    }

    private void handleGET(PrintWriter out, BufferedOutputStream byteOut, HttpRequest request, boolean isHead) throws IOException {
        HttpResponse response = new HttpResponse();
        String route = request.getRoute();

        if (!routeExists(route)) {
            throwNotFoundRequest(out, response);
            return;
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
                File file = new File("webpage/" + request.getRoute().substring(1));
                int length = (int) file.length();
                String lastModified = request.getHeader().get("If-Modified-Since");

                if (lastModified != null) {
                    Date dateHeader = new SimpleDateFormat("EEE,dd MMM yyyy kk:mm:ss zzz").parse(lastModified);
                    Date fileMod = new Date(file.lastModified());
                    if (fileMod.before(dateHeader)) {
                        throwNotModifiedRequest(out, response);
                        return;
                    }
                }

                response.setContentType("image/jpg");
                response.setResponseMessage("OK");
                response.setResponseCode(200);

                response.setContentLength(length);

                handleOutGoingHeader(out, response);

                if (!isHead) {
                    writeFileInChunksToStream(file, byteOut);
                    byteOut.flush();
                }
            }
        } catch (FileNotFoundException e) {
            throwNotFoundRequest(out, response);
        } catch (ParseException e) {
            throwBadRequest(out, response);
        }
    }

    private void handlePOST(PrintWriter out, HttpRequest request) {
        //TODO
    }

    private void handlePUT(PrintWriter out, HttpRequest request) {
        //TODO
    }

    private void throwBadRequest(PrintWriter out, HttpResponse resp) throws IOException {
        resp.setResponseCode(400);
        resp.setResponseMessage("Bad Request");
        resp.setContentType("text/plain");

        handleOutGoingHeader(out, resp);
    }

    private void throwNotFoundRequest(PrintWriter out, HttpResponse resp) throws IOException {
        resp.setResponseMessage("Not found");
        resp.setResponseCode(404);
        resp.setContentType("text/plain");

        handleOutGoingHeader(out, resp);
    }

    private void throwNotModifiedRequest(PrintWriter out, HttpResponse resp) throws IOException {
        resp.setResponseMessage("Not Modified");
        resp.setResponseCode(304);
        resp.setContentType("text/plain");

        handleOutGoingHeader(out, resp);
    }

    private void throwServerErrorRequest(PrintWriter out, HttpResponse resp) {
        try {
            resp.setResponseMessage("Server Error");
            resp.setResponseCode(500);
            resp.setContentType("text/plain");

            handleOutGoingHeader(out, resp);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private boolean routeExists(String route) {
        File directory = new File("webpage");
        File[] listOfFiles = directory.listFiles();

        //root
        if (route.equals("/")) return true;

        //other routes
        if (listOfFiles != null)
            for(File f : listOfFiles)
                if (f.isFile())
                    if (f.getName().equals(route.substring(1)))
                        return true;
        return false;
    }

    private void writeFileInChunksToStream(File file, BufferedOutputStream byteOut) throws IOException {

        //we create a stream and directly read from the file and write into the outgoing stream
        FileInputStream fileStream =  new FileInputStream(file);

        int count;
        byte[] buffer = new byte[8192]; //chunk size
        while ((count = fileStream.read(buffer)) > 0)
        {
            byteOut.write(buffer, 0, count);
        }

        fileStream.close();
    }
}
