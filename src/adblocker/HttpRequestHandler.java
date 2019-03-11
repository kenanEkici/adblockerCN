package adblocker;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class HttpRequestHandler implements Runnable {

    private Socket client;

    protected HttpRequestHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        DataOutputStream out = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new DataOutputStream(client.getOutputStream());

            //---- READING CLIENT REQUEST HEADER
            HttpRequest request = new HttpRequest();
            String line = in.readLine();
            while (line != null && !line.equals("")) {
                request.appendToHeader(line);
                line = in.readLine();
            }

            // Throw 400 if header does not contain Host for HTTP == 1.1
            if (request.getProtocolVersion().equals("HTTP/1.1") && request.getHeader().get("Host") == null) {
                throwBadRequest(out, new HttpResponse());
            } else {

                //---- HANDLE REQUESTS BY METHOD
                switch (request.getHttpMethod()) {
                    case "GET": handleGET(out, request, false); break;
                    case "HEAD": handleGET(out, request, true); break;
                    case "POST": handlePOST(out, request); break;
                    case "PUT": handlePUT(out, request); break;
                    default: throwBadRequest(out, new HttpResponse());
                }
            }

            out.close();
            in.close();
            client.close();

        } catch (IOException ex) {
            throwServerErrorRequest(out, new HttpResponse());
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Writes the header for outgoing responses
     * @param out : Stream to write header to client
     * @param resp : Response to read header data from
     * @throws IOException
     */
    private void handleOutGoingHeader(DataOutputStream out, HttpResponse resp) throws IOException {
        //---- WRITING RESPONSE HEADER
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();

        out.writeBytes(resp.getProtocolVersion()+ " " + resp.getResponseCode() + " " + resp.getResponseMessage()+ "\r\n");
        out.writeBytes("Date: " + dateFormat.format(date) + "\r\n");
        if (resp.getContentType()!= null) out.writeBytes("Content-Type: " + resp.getContentType() + "\r\n");
        if (!resp.getTransferEncoding().equals("none")) out.writeBytes("Transfer-Encoding: " + resp.getTransferEncoding() + "\r\n");
        else out.writeBytes("Content-Length: " + resp.getContentLength() + "\r\n");

        //END OF HEADER
        out.writeBytes("\r\n");
        out.flush();
    }

    /**
     * Handles a client GET and HEAD request.
     * @param out : Stream to write bytes to
     * @param request : Request to process
     * @param isHead : Determines whether the request is a HEAD request
     * @throws IOException throws IO Exception
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
                    out.writeBytes(response.getBody() + "\r\n");
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
                    writeFileInChunksToStream(file, out);
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
     * Handles a client POST request.
     * @param out : Stream to write bytes to
     * @param request : Request to process
     * @throws IOException throws IO Exception
     */
    private void handlePOST(DataOutputStream out, HttpRequest request) {
        //TODO
    }

    /**
     * Handles a client PUT request.
     * @param out : Stream to write bytes to
     * @param request : Request to process
     * @throws IOException throws IO Exception
     */
    private void handlePUT(DataOutputStream out, HttpRequest request) {
        //TODO
    }

    /**
     * Assembles header data for 400 BAD REQUEST
     * @param out : Stream to write bytes to
     * @param resp : Response to wrap data around
     * @throws IOException throws IO Exception
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
     * @throws IOException throws IO Exception
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
     * @throws IOException throws IO Exception
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

    /**
     * Method to read bytes from given file and write to outgoing stream
     * @param file : file to be read from
     * @param byteOut : stream to write to
     * @throws IOException throws IO Exception
     */
    private void writeFileInChunksToStream(File file, DataOutputStream byteOut) throws IOException {

        // READING FROM FILE STREAM / WRITING TO DATA OUTPUT STREAM
        FileInputStream fileStream =  new FileInputStream(file);

        int count;
        byte[] buffer = new byte[8192]; // chunk size
        while ((count = fileStream.read(buffer)) > 0)
        {
            byteOut.write(buffer, 0, count);
        }

        fileStream.close();
    }
}
