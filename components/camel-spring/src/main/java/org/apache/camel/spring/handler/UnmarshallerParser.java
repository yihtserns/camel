/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import com.github.yihtserns.jaxbean.unmarshaller.api.BeanHandler;
import com.github.yihtserns.jaxbean.unmarshaller.api.SpringBeanHandler;
import java.util.List;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.spi.NamespaceAware;
import org.apache.camel.spring.CamelEndpointFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 *
 * @author yihtserns
 */
class UnmarshallerParser extends AbstractBeanDefinitionParser {

    private Logger log = LoggerFactory.getLogger(getClass());
    private boolean includeComplexTypes = true;
    private Class<?> customBeanClass = null;

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        try {
            return (AbstractBeanDefinition) Unmarshaller.INSTANCE.unmarshal(element, new BeanHandler<BeanDefinitionBuilder>() {
                private NamespacesCache namespacesCache = new NamespacesCache();

                @Override
                public BeanDefinitionBuilder createBean(Class<?> beanClass, Element element) {
                    if (customBeanClass != null) {
                        beanClass = customBeanClass;
                    }
                    return SpringBeanHandler.INSTANCE.createBean(beanClass, element);
                }

                @Override
                public void setBeanProperty(BeanDefinitionBuilder bean, String propertyName, Object propertyValue) {
                    boolean complexType = !(propertyValue instanceof String);
                    if (complexType && !includeComplexTypes) {
                        return;
                    }
                    if (propertyName.equals("uri") || propertyName.endsWith("Uri")) {
                        String stringValue = (String) propertyValue;
                        // remove all double spaces
                        String changed = stringValue.replaceAll("\\s{2,}", "");
                        if (!stringValue.equals(changed)) {
                            log.debug("Removed whitespace noise from attribute {} -> {}", stringValue, changed);
                            propertyValue = changed;
                        }
                    }
                    SpringBeanHandler.INSTANCE.setBeanProperty(bean, propertyName, propertyValue);
                }

                @Override
                public AbstractBeanDefinition unmarshalWith(XmlAdapter xmlAdapter, Object from) {
                    return SpringBeanHandler.INSTANCE.unmarshalWith(xmlAdapter, from);
                }

                @Override
                public ManagedList<Object> postProcessList(List<Object> unprocessedList) {
                    return SpringBeanHandler.INSTANCE.postProcessList(unprocessedList);
                }

                @Override
                public AbstractBeanDefinition postProcess(BeanDefinitionBuilder bean, List<Attr> customAttributes, List<Element> customElements) {
                    AbstractBeanDefinition rawBd = bean.getRawBeanDefinition();
                    Class<?> beanClass = rawBd.getBeanClass();
                    Element element = (Element) rawBd.getSource();
                    if (NamespaceAware.class.isAssignableFrom(beanClass)) {
                        Namespaces namespaces = namespacesCache.getOrGenerateNamespaces((Element) element.getParentNode());
                        bean.addPropertyValue("namespaces", namespaces.getNamespaces());
                    }
                    return SpringBeanHandler.INSTANCE.postProcess(bean, customAttributes, customElements);
                }
            });
        } catch (Exception ex) {
            String msg = String.format("Unable to marshal <%s/>", element.getLocalName());
            parserContext.getReaderContext().fatal(msg, element, ex);
            return null;
        }
    }

    public static UnmarshallerParser create() {
        return new UnmarshallerParser();
    }

    public static UnmarshallerParser dynamicEndpointParser() {
        UnmarshallerParser parser = new UnmarshallerParser();
        parser.customBeanClass = CamelEndpointFactoryBean.class;
        parser.includeComplexTypes = false;
        return parser;
    }
}
