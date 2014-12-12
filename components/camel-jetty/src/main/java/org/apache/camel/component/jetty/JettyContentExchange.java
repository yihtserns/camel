package org.apache.camel.component.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import org.apache.camel.AsyncCallback;
import org.eclipse.jetty.client.HttpClient;

public interface JettyContentExchange {

    void setCallback(AsyncCallback callback);

    byte[] getBody();

    String getUrl();

    void setRequestContentType(String contentType);

    int getResponseStatus();

    void setMethod(String method);

    void setTimeout(long timeout);

    void setURL(String url);

    void setRequestContent(byte[] byteArray);

    void setRequestContent(String data, String charset) throws UnsupportedEncodingException;

    void setRequestContent(InputStream ins);

    void addRequestHeader(String key, String s);

    void send(HttpClient client) throws IOException;

    byte[] getResponseContentBytes();

    Map<String, Collection<String>> getResponseHeaders();

    void setSupportRedirect(boolean supportRedirect);

}
