package adblocker.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ByteWriter {

    /**
     * Reades bytes from stream and writes them in a file
     * @param in : Stream to read bytes from
     * @param fileName : File to write bytes to
     * @param contentLength : Length of byte array
     * @param isChunked : Indicates whether the data encoded as chunks
     * @throws IOException IOException
     */
    public void writeToFile(DataInputStream in, String fileName, int contentLength, boolean isChunked) throws IOException {

        // split actual file and its directory structure
        int lastSlash = fileName.lastIndexOf("/");
        String directories = fileName.substring(0, lastSlash);
        File directory = new File("websiteOutputs/" + directories);

        // create underlying directory structure if it does not exist
        if (!directory.exists()) {
            directory.mkdirs();
        }
        OutputStream fileWriter = new FileOutputStream("websiteOutputs/" + fileName);

        //----WRITING BYTES TO FILE

        // BY CONTENT LENGTH
        if (!isChunked) {
            int totalRead = 0;
            byte[] buffer = new byte[8192]; // chunk size
            while (totalRead < contentLength) {
                int count = in.read(buffer);
                fileWriter.write(buffer, 0, count);
                totalRead += count;
            }
        }
        // BY CHUNKS
        else {
            int count;
            byte[] buffer = new byte[8192]; // chunk size
            while ((count = in.read(buffer)) > 0) {
                fileWriter.write(buffer, 0, count);
            }
        }

        //----END OF WRITING TO FILE

        fileWriter.close();
    }

    /**
     * Method to write a String to an HTML file.
     *
     * @param body : the String to be written
     */
    public void writeToHtml(String body) {
        try {
            Files.write(Paths.get("websiteOutputs/index.html"), body.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
