package adblocker;

public class HttpRequest extends HttpMessage {

    private String httpMethod;
    private String route;

    public HttpRequest(String httpMethod, String route, String version) {
        super.setProtocolVersion(version);
        setHttpMethod(httpMethod);
        setHttpRoute(route);
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setHttpRoute(String route) {
        this.route = route;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRoute() {
        return route;
    }

}
