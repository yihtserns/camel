package org.apache.camel.component.jetty8;

import java.util.concurrent.Executor;

import org.apache.camel.component.jetty.CamelHttpClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ThreadPool;

public class CamelHttpClient8 extends CamelHttpClient {

    public CamelHttpClient8(SslContextFactory sslContextFactory) {
        super(sslContextFactory);
        setConnectorType();
    }
    
    private void setConnectorType() {
        try {
            HttpClient.class.getMethod("setConnectorType", Integer.TYPE).invoke(this, 2);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    protected boolean hasThreadPool() {
        try {
            return getClass().getMethod("getThreadPool").invoke(this) != null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void setThreadPoolOrExecutor(Executor pool) {
        try {
            getClass().getMethod("setThreadPool", ThreadPool.class).invoke(this, pool);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void setProxy(String host, int port) {
        try {
            // setProxy(new org.eclipse.jetty.client.Address(host, port));
            Class<?> c = Class.forName("org.eclipse.jetty.client.Address");
            Object o = c.getConstructor(String.class, Integer.TYPE).newInstance(host, port);
            this.getClass().getMethod("setProxy", c).invoke(this, o);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
