/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.spring.handler;

import java.lang.reflect.Method;
import org.apache.camel.core.xml.CamelProxyFactoryDefinition;
import org.apache.camel.core.xml.CamelServiceExporterDefinition;
import org.apache.camel.spring.CamelContextFactoryBean;
import org.apache.camel.spring.remoting.CamelProxyFactoryBean;
import org.apache.camel.spring.remoting.CamelServiceExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Camel namespace for the spring XML configuration file.
 */
public class CamelNamespaceHandler extends NamespaceHandlerSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CamelNamespaceHandler.class);
    private UnmarshallerParser genericUnmarshallerParser = UnmarshallerParser.create();

    public void init() {
        registerBeanDefinitionParser("restContext", genericUnmarshallerParser);
        registerBeanDefinitionParser("routeContext", genericUnmarshallerParser);
        registerBeanDefinitionParser("endpoint", genericUnmarshallerParser);
        registerBeanDefinitionParser("sslContextParameters", genericUnmarshallerParser);
        registerBeanDefinitionParser("template", genericUnmarshallerParser);
        registerBeanDefinitionParser("consumerTemplate", genericUnmarshallerParser);
        registerBeanDefinitionParser("threadPool", genericUnmarshallerParser);
        registerBeanDefinitionParser("redeliveryPolicyProfile", genericUnmarshallerParser);
        registerBeanDefinitionParser("keyStoreParameters", genericUnmarshallerParser);
        registerBeanDefinitionParser("secureRandomParameters", genericUnmarshallerParser);
        registerBeanDefinitionParser("errorHandler", genericUnmarshallerParser);
        registerBeanDefinitionParser("proxy", new BeanDefinitionParser(CamelProxyFactoryBean.class, false));
        registerBeanDefinitionParser("export", new BeanDefinitionParser(CamelServiceExporter.class, false));

        // camel context
        boolean osgi = false;
        Class<? extends CamelContextFactoryBean> cl = CamelContextFactoryBean.class;
        // These code will try to detected if we are in the OSGi environment.
        // If so, camel will use the OSGi version of CamelContextFactoryBean to create the CamelContext.
        try {
            // Try to load the BundleActivator first
            Class.forName("org.osgi.framework.BundleActivator");
            Class<?> c = Class.forName("org.apache.camel.osgi.Activator");
            Method mth = c.getDeclaredMethod("getBundle");
            Object bundle = mth.invoke(null);
            if (bundle != null) {
                cl = Class.forName("org.apache.camel.osgi.CamelContextFactoryBean").asSubclass(CamelContextFactoryBean.class);
                osgi = true;
            }
        } catch (Throwable t) {
            // not running with camel-core-osgi so we fallback to the regular factory bean
            LOG.trace("Cannot find class so assuming not running in OSGi container: " + t.getMessage());
        }
        if (osgi) {
            LOG.info("OSGi environment detected.");
        }
        LOG.debug("Using {} as CamelContextBeanDefinitionParser", cl.getCanonicalName());
        CamelContextBeanDefinitionParser camelContextParser = new CamelContextBeanDefinitionParser(cl);
        camelContextParser.beanClass2ReplacementClass.put(CamelProxyFactoryDefinition.class, CamelProxyFactoryBean.class);
        camelContextParser.beanClass2ReplacementClass.put(CamelServiceExporterDefinition.class, CamelServiceExporter.class);

        registerBeanDefinitionParser("camelContext", camelContextParser);
    }
}
