package adblocker.clientServer;

import adblocker.httpMessages.HttpRequest;
import adblocker.httpMessages.HttpResponse;
import adblocker.io.ByteReader;
import adblocker.io.ByteWriter;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequestHandler implements Runnable {

    private final Socket client;
    private final DataInputStream in;
    private final DataOutputStream out;
    private boolean dispose = true;

    HttpRequestHandler(Socket client, DataInputStream in, DataOutputStream out) {
        this.client = client;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {

            while(true) {

                //---- READING CLIENT REQUEST HEADER
                HttpRequest request = new HttpRequest();
                client.setKeepAlive(handleIncomingHeader(in, request));

                // Throw 400 if header does not contain Host for HTTP == 1.1
                if (request.getProtocolVersion().equals("HTTP/1.1") && request.getHeader().get("Host") == null) {
                    throwBadRequest(out, new HttpResponse());
                } else {

                    switch (request.getHttpMethod()) {
                        case "GET": handleGET(out, request, false); break;
                        case "HEAD": handleGET(out, request, true); break;
                        case "POST": handlePUTPOST(out, in, request, false); dispose=false; break;
                        case "PUT": handlePUTPOST(out, in, request, true); dispose=false; break;
                        default:
                            throwMethodNotImplementedResponse(out, new HttpResponse());
                    }
                }
                // IF KEEP ALIVE IS SET, CONTINUE
                if (!client.getKeepAlive()) {
                    break;
                }
            }
        } catch (IOException ex) {
            throwServerErrorRequest(out, new HttpResponse());
            System.out.println(ex.getMessage());
        }

        try {
            if (dispose){
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Reads the header of incoming requests
     * @param in : Stream to read from
     * @param request : Reques to wrap data into
     * @return : whether the Keep Alive flag is set or not
     * @throws IOException throws IOException
     */
    private boolean handleIncomingHeader(DataInputStream in, HttpRequest request) throws IOException {
        char c = (char)in.readByte();
        String line = "" + c;

        while (true) {
            while (!line.endsWith("\r\n") ) {
                c = (char)in.readByte();
                line += c;
            }
            if (line.equals("\r\n"))
                break;
            String newLine = line.substring(0,line.indexOf("\r\n"));
            request.appendToHeader(newLine);
            line = "";
        }
        return request.getHeader().get("Connection").equals("Keep Alive");
    }

    /**
     * Writes the header for outgoing responses
     * @param out : Stream to write header to client
     * @param resp : Response to read header data from
     * @throws IOException throws IOException
     */
    private void handleOutGoingHeader(DataOutputStream out, HttpResponse resp) throws IOException {
        //---- WRITING RESPONSE HEADER
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        out.writeBytes(resp.getProtocolVersion()+ " " + resp.getResponseCode() + " " + resp.getResponseMessage()+ "\r\n");
        out.writeBytes("Date: " + dateFormat.format(date) + "\r\n");
        out.writeBytes("Content-Type: " + resp.getContentType() + "\r\n");
        out.writeBytes("Content-Length: " + resp.getContentLength() + "\r\n");

        //END OF HEADER
        out.writeBytes("\r\n");
        out.flush();
    }

    /**
     * Handles a client GET and HEAD request.
     * @param out : Stream to write bytes to
     * @param request : Request to process
     * @param isHead : Determines whether the request is a HEAD request
     * @throws IOException throws io Exception
     */
    private void handleGET(DataOutputStream out, HttpRequest request, boolean isHead) throws IOException {
        HttpResponse response = new HttpResponse();
        String route = request.getRoute();

        // If given route (route excluded) does not exist, throw 404
        if (!routeExists(route)) {
            throwNotFoundRequest(out, response);
            return;
        }

        try {
            // Handle root index
            if (request.getRoute().equals("/") || request.getRoute().equals("index.html")) {

                // Write HTML file
                BufferedReader in = new BufferedReader(new FileReader("webpage/index.html"));
                response.setBody(in.lines().collect(Collectors.joining()));
                response.setContentType("text/html");
                response.setResponseMessage("OK");
                response.setResponseCode(200);
                response.setContentLength(response.getBody().getBytes().length);

                handleOutGoingHeader(out, response);

                if (!isHead) {
                    //---- WRITE RESPONSE BODY TO CLIENT
                    out.writeBytes(response.getBody());
                    out.flush();
                }
            }
            // Handle files (images)
            else {
                File file = new File("webpage/" + request.getRoute().substring(1));
                int length = (int) file.length();
                String lastModified = request.getHeader().get("If-Modified-Since");

                // Throw 304 if last modified header is rejected
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
                    //---- WRITE RESPONSE BODY TO CLIENT
                    ByteWriter writer = new ByteWriter();
                    writer.writeFileToStream(file, out);
                    out.flush();
                }
            }
        } catch (FileNotFoundException e) {
            throwNotFoundRequest(out, response);
        } catch (ParseException e) {
            throwBadRequest(out, response);
        }
    }

    /**
     * Handles a client POST or PUT request.
     * @param out : Stream to write bytes to
     * @param request : Request to process
     * @throws IOException throws io Exception
     */
    private void handlePUTPOST(DataOutputStream out, DataInputStream in, HttpRequest request, boolean isPutRequest) throws IOException {
        Map<String, String> header = request.getHeader();
        HttpResponse response = new HttpResponse();
        ByteReader reader = new ByteReader();

        if (header.get("Content-Type").contains("text/plain")) {
            String body = "";
            String length = header.get("Content-Length");

            // read byte per byte
            if (length != null) {
                body = reader.readBodyByLength(in, length);
                System.out.println(body);
            }

            ByteWriter writer = new ByteWriter();
            writer.writeToFile("clientInputs/amazingFile.txt", body, !isPutRequest);
            response.setResponseCode(200);
            response.setContentType("text/html");
            response.setResponseMessage("OK");
            response.setContentLength(0);

            handleOutGoingHeader(out, response);
        }
    }

    /**
     * Assembles header data for 400 BAD REQUEST
     * @param out : Stream to write bytes to
     * @param resp : Response to wrap data around
     * @throws IOException throws io Exception
     */
    private void throwBadRequest(DataOutputStream out, HttpResponse resp) throws IOException {
        resp.setResponseCode(400);
        resp.setResponseMessage("Bad Request");
        resp.setContentType("text/plain");

        handleOutGoingHeader(out, resp);
    }

    /**
     * Assembles header data for 404 NOT FOUND
     * @param out : Stream to write bytes to
     * @param resp : Response to wrap data around
     * @throws IOException throws io Exception
     */
    private void throwNotFoundRequest(DataOutputStream out, HttpResponse resp) throws IOException {
        resp.setResponseMessage("Not found");
        resp.setResponseCode(404);
        resp.setContentType("text/plain");

        handleOutGoingHeader(out, resp);
    }

    /**
     * Assembles header data for 304 NOT MODIFIED
     * @param out : Stream to write bytes to
     * @param resp : Response to wrap data around
     * @throws IOException throws io Exception
     */
    private void throwNotModifiedRequest(DataOutputStream out, HttpResponse resp) throws IOException {
        resp.setResponseMessage("Not Modified");
        resp.setResponseCode(304);
        resp.setContentType("text/plain");

        handleOutGoingHeader(out, resp);
    }

    /**
     * Assembles header data for 400 BAD REQUEST
     * @param out : Stream to write bytes to
     * @param resp : Response to wrap data around
     */
    private void throwServerErrorRequest(DataOutputStream out, HttpResponse resp) {
        try {
            resp.setResponseMessage("Server Error");
            resp.setResponseCode(500);
            resp.setContentType("text/plain");

            handleOutGoingHeader(out, resp);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Assembles header data for 501 NOT IMPLEMENTED
     * @param out : Stream to write bytes to
     * @param resp : Response to wrap data around
     */
    private void throwMethodNotImplementedResponse(DataOutputStream out, HttpResponse resp) {
        try {
            resp.setResponseMessage("Not Implemented!");
            resp.setResponseCode(501);
            resp.setContentType("text/plain");

            handleOutGoingHeader(out, resp);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Checks if certain route provided by client exists in directory structure.
     * @param route : route to check for
     * @return : true if route exists, false if not
     */
    private boolean routeExists(String route) {
        File directory = new File("webpage");
        File[] listOfFiles = directory.listFiles();

        // Exclude root
        if (route.equals("/")) return true;

        // Check files within webpage directory
        if (listOfFiles != null)
            for(File f : listOfFiles)
                if (f.isFile())
                    if (f.getName().equals(route.substring(1)))
                        return true;
        return false;
    }
}
