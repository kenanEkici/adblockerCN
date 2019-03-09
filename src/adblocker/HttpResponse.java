package adblocker;

public class HttpResponse extends HttpMessage {

    private int responseCode;
    private String responseMessage;
    private String contentType;
    private int contentLength;
    private String transferEncoding = "none";

    public void appendToHeader(String line) {
        int colon = line.indexOf(":");
        if (!(colon == -1)) {
            String key = line.substring(0,colon);
            String value = line.substring(colon+1);
            super.appendToHeader(key.trim(), value.trim());
        } else {
            int code = line.indexOf(" ");
            int codeAfter = line.lastIndexOf(" ");
            setProtocolVersion(line.substring(0, code));
            setResponseCode(Integer.parseInt(line.substring(code+1, codeAfter)));
            setResponseMessage(line.substring(codeAfter));
        }
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public String getContentType() {
        return contentType;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getTransferEncoding() {
        return transferEncoding;
    }

    public void setTransferEncoding(String transferEncoding) {
        this.transferEncoding = transferEncoding;
    }
}
