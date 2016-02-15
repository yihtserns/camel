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
package org.apache.camel.spring;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.camel.ErrorHandlerFactory;

import org.apache.camel.LoggingLevel;
import org.apache.camel.model.IdentifiedType;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * The &lt;errorHandler&gt; tag element.
 *
 * @version 
 */
@XmlRootElement(name = "errorHandler")
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorHandlerFactoryBean extends IdentifiedType
        implements InitializingBean, FactoryBean<ErrorHandlerFactory>, BeanFactoryAware {

    @XmlAttribute
    private ErrorHandlerType type = ErrorHandlerType.DefaultErrorHandler;
    @XmlAttribute
    private String deadLetterUri;
    @XmlAttribute
    private Boolean deadLetterHandleNewException;
    @XmlAttribute
    private LoggingLevel level;
    @XmlAttribute
    private LoggingLevel rollbackLoggingLevel;
    @XmlAttribute
    private String logName;
    @XmlAttribute
    private Boolean useOriginalMessage;
    @XmlAttribute
    private String transactionTemplateRef;
    @XmlAttribute
    private String transactionManagerRef;
    @XmlAttribute
    private String onRedeliveryRef;
    @XmlAttribute
    private String onPrepareFailureRef;
    @XmlAttribute
    private String retryWhileRef;
    @XmlAttribute
    private String redeliveryPolicyRef;
    @XmlAttribute
    private String executorServiceRef;
    @XmlElement
    private CamelRedeliveryPolicyFactory redeliveryPolicy;
    @XmlTransient
    private BeanFactory beanFactory;
    @XmlTransient
    private ErrorHandlerFactory instance;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.instance = type.getTypeAsClass().newInstance();
        BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(instance);

        if (deadLetterUri != null) {
            bean.setPropertyValue("deadLetterUri", deadLetterUri);
        }
        if (deadLetterHandleNewException != null) {
            bean.setPropertyValue("deadLetterHandleNewException", deadLetterHandleNewException);
        }
        if (level != null) {
            bean.setPropertyValue("level", level);
        }
        if (logName != null) {
            bean.setPropertyValue("logName", logName);
        }
        if (useOriginalMessage != null) {
            bean.setPropertyValue("useOriginalMessage", useOriginalMessage);
        }
        if (transactionTemplateRef != null) {
            bean.setPropertyValue("transactionTemplate", resolveBean(transactionTemplateRef));
        }
        if (transactionManagerRef != null) {
            bean.setPropertyValue("transactionManager", resolveBean(transactionManagerRef));
        }
        if (onRedeliveryRef != null) {
            bean.setPropertyValue("onRedelivery", resolveBean(onRedeliveryRef));
        }
        if (onPrepareFailureRef != null) {
            bean.setPropertyValue("onPrepareFailure", resolveBean(onPrepareFailureRef));
        }
        if (retryWhileRef != null) {
            bean.setPropertyValue("retryWhileRef", retryWhileRef);
        }
        if (redeliveryPolicyRef != null) {
            bean.setPropertyValue("redeliveryPolicy", resolveBean(redeliveryPolicyRef));
        }
        if (executorServiceRef != null) {
            bean.setPropertyValue("executorServiceRef", executorServiceRef);
        }
        if (redeliveryPolicy != null) {
            bean.setPropertyValue("redeliveryPolicy", redeliveryPolicy.getObject());
        }
    }

    private Object resolveBean(String beanName) {
        return beanFactory.getBean(beanName);
    }

    @Override
    public ErrorHandlerFactory getObject() throws Exception {
        return instance;
    }

    @Override
    public Class<? extends ErrorHandlerFactory> getObjectType() {
        return type.getTypeAsClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setType(ErrorHandlerType type) {
        this.type = type;
    }

    public void setDeadLetterUri(String deadLetterUri) {
        this.deadLetterUri = deadLetterUri;
    }

    public void setDeadLetterHandleNewException(boolean deadLetterHandleNewException) {
        this.deadLetterHandleNewException = deadLetterHandleNewException;
    }

    public void setLevel(LoggingLevel level) {
        this.level = level;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public void setUseOriginalMessage(boolean useOriginalMessage) {
        this.useOriginalMessage = useOriginalMessage;
    }

    public void setTransactionTemplateRef(String transactionTemplateRef) {
        this.transactionTemplateRef = transactionTemplateRef;
    }

    public void setTransactionManagerRef(String transactionManagerRef) {
        this.transactionManagerRef = transactionManagerRef;
    }

    public void setOnRedeliveryRef(String onRedeliveryRef) {
        this.onRedeliveryRef = onRedeliveryRef;
    }

    public void setOnPrepareFailureRef(String onPrepareFailureRef) {
        this.onPrepareFailureRef = onPrepareFailureRef;
    }

    public void setRetryWhileRef(String retryWhileRef) {
        this.retryWhileRef = retryWhileRef;
    }

    public void setRedeliveryPolicyRef(String redeliveryPolicyRef) {
        this.redeliveryPolicyRef = redeliveryPolicyRef;
    }

    public void setExecutorServiceRef(String executorServiceRef) {
        this.executorServiceRef = executorServiceRef;
    }

    public void setRedeliveryPolicy(CamelRedeliveryPolicyFactory redeliveryPolicy) {
        this.redeliveryPolicy = redeliveryPolicy;
    }
}
