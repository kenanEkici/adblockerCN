package adblocker.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ByteWriter {

    /**
     * Reads bytes from stream in chunks and writes them in a file
     * @param in : Stream to read bytes from
     * @param fileName : File to write bytes to
     * @throws IOException IOException
     */
    public void writeToFileByChunks(DataInputStream in, String fileName) throws IOException {

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


        // BY CHUNKS
        while (true) {

            String chunkLength = "";
            chunkLength += (char) in.readByte();
            while (!chunkLength.contains("\r\n"))
                chunkLength += (char) in.readByte();

            if (chunkLength == null)
                break;

            byte[] buffer;// chunk size
            int chunksize = Integer.parseInt(chunkLength.substring(0, chunkLength.length() - 2), 16);
            if (chunksize == 0)
                break;
            buffer = new byte[chunksize];

            in.read(buffer);
            fileWriter.write(buffer, 0, chunksize);

            String newLine = "";
            newLine += (char) in.readByte();
            while (!newLine.contains("\r\n"))
                newLine += (char) in.readByte();
        }

        //----END OF WRITING TO FILE

        fileWriter.close();
    }

    /**
     * Reads bytes from stream by content length and writes them in a file
     * @param in : Stream to read bytes from
     * @param fileName : File to write bytes to
     * @param contentLength : Length of byte array
     * @throws IOException IOException
     */
    public void writeToFileByLength(DataInputStream in, String fileName, int contentLength) throws IOException {

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
        int totalRead = 0;
        byte[] buffer = new byte[8192]; // chunk size
        while (totalRead < contentLength) {
            int count = in.read(buffer);
            fileWriter.write(buffer, 0, count);
            totalRead += count;
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

    /**
     * Method to read bytes from given file and write to outgoing stream
     * @param file : file to be read from
     * @param byteOut : stream to write to
     * @throws IOException throws io Exception
     */
    public void writeFileToStream(File file, DataOutputStream byteOut) throws IOException {

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

    /**
     * Method to write content to file with options to append or create new file
     * @param path : path of file
     * @param content : content to write
     * @param append : whether to append the content
     */
    public void writeToFile(String path, String content, boolean append) {
        try {
            File directory = new File("clientInputs");
            OpenOption[] openOptions;
            if(append){
                openOptions = new OpenOption[]{
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND };
            }
            else{

                openOptions = new OpenOption[]{
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING };
            }
            if (!directory.exists()) {
                directory.mkdirs();
            }
            Files.write(Paths.get(path), content.getBytes(), openOptions);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
