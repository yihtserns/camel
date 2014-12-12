package org.apache.camel.component.jetty;

import org.eclipse.jetty.util.ssl.SslContextFactory;

public class JettyHttpComponent9 extends JettyHttpComponent {

    protected CamelHttpClient createCamelHttpClient(SslContextFactory sslContextFactory) {
        return new CamelHttpClient9(sslContextFactory);
    }

}
