package adblocker;

import java.util.HashMap;
import java.util.Map;

public abstract class HttpMessage {

    private Map<String, String> header = new HashMap<>();
    private String body;

    private String protocolVersion;
    private int responseCode;
    private String responseMessage;

    public Map<String, String> getHeader() {
        return header;
    }

    public void appendToHeader(String key, String value) {
        this.header.put(key, value);
    }

    public String getBody() { return body; }

    public void setBody(String body) {
        this.body = body;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
}
