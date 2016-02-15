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
package org.apache.camel.util.spring;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.camel.CamelContext;
import org.apache.camel.core.xml.util.jsse.AbstractKeyManagersParametersFactoryBean;
import org.apache.camel.spring.util.CamelContextResolverHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class KeyManagersParametersFactoryBean extends AbstractKeyManagersParametersFactoryBean 
        implements ApplicationContextAware {

    @XmlElement
    KeyStoreParametersFactory keyStore;

    @XmlTransient
    private ApplicationContext applicationContext;

    @Override
    public KeyStoreParametersFactory getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(KeyStoreParametersFactory keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    protected CamelContext getCamelContextWithId(String camelContextId) {
        return CamelContextResolverHelper.getCamelContextWithId(applicationContext, camelContextId);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
