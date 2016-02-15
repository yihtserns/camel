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
package org.apache.camel.core.xml.util.jsse;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.camel.util.jsse.BaseSSLContextParameters;

@XmlTransient
public abstract class AbstractBaseSSLContextParametersFactoryBean<T extends BaseSSLContextParameters> extends AbstractJsseUtilFactoryBean<T> {

    @XmlElement
    private CipherSuitesParametersDefinition cipherSuites;
    
    @XmlElement
    private FilterParametersDefinition cipherSuitesFilter;
    
    @XmlElement
    private SecureSocketProtocolsParametersDefinition secureSocketProtocols;
    
    @XmlElement
    private FilterParametersDefinition secureSocketProtocolsFilter;
    
    @XmlAttribute
    private String sessionTimeout;
    
    @XmlTransient
    private T instance;
    
    @Override
    public final T getObject() throws Exception {
        if (this.isSingleton()) {
            if (instance == null) { 
                instance = createInstanceInternal();   
            }
            
            return instance;
        } else {
            return createInstanceInternal();
        } 
    }
    
    protected abstract T createInstance() throws Exception;
    
    private T createInstanceInternal() throws Exception {
        T newInstance = createInstance();
        newInstance.setCamelContext(getCamelContext());

        if (cipherSuites != null) {
            newInstance.setCipherSuites(cipherSuites.create());
        }
        
        if (cipherSuitesFilter != null) {
            newInstance.setCipherSuitesFilter(cipherSuitesFilter.create(getCamelContext()));
        }
        
        if (secureSocketProtocols != null) {
            newInstance.setSecureSocketProtocols(secureSocketProtocols.create());
        }
        
        if (secureSocketProtocolsFilter != null) {
            newInstance.setSecureSocketProtocolsFilter(secureSocketProtocolsFilter.create(getCamelContext()));
        }
        
        if (sessionTimeout != null) {
            newInstance.setSessionTimeout(sessionTimeout);
        }

        return newInstance;
    }

    public CipherSuitesParametersDefinition getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(CipherSuitesParametersDefinition cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public FilterParametersDefinition getCipherSuitesFilter() {
        return cipherSuitesFilter;
    }

    public void setCipherSuitesFilter(FilterParametersDefinition cipherSuitesFilter) {
        this.cipherSuitesFilter = cipherSuitesFilter;
    }

    public SecureSocketProtocolsParametersDefinition getSecureSocketProtocols() {
        return secureSocketProtocols;
    }

    public void setSecureSocketProtocols(SecureSocketProtocolsParametersDefinition secureSocketProtocols) {
        this.secureSocketProtocols = secureSocketProtocols;
    }

    public FilterParametersDefinition getSecureSocketProtocolsFilter() {
        return secureSocketProtocolsFilter;
    }

    public void setSecureSocketProtocolsFilter(FilterParametersDefinition secureSocketProtocolsFilter) {
        this.secureSocketProtocolsFilter = secureSocketProtocolsFilter;
    }

    public String getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(String sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
