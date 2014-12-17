package org.apache.camel.component.jetty8;

import org.apache.camel.component.jetty.CamelHttpClient;
import org.apache.camel.component.jetty.JettyHttpComponent;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class JettyHttpComponent8 extends JettyHttpComponent {

    protected CamelHttpClient createCamelHttpClient(SslContextFactory sslContextFactory) {
        return new CamelHttpClient8(sslContextFactory);
    }

}
