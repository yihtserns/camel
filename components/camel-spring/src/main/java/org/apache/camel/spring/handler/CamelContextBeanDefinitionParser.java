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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.namespace.QName;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.impl.DefaultCamelContextNameStrategy;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.SendDefinition;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.spi.NamespaceAware;
import org.apache.camel.spring.CamelBeanPostProcessor;
import org.apache.camel.spring.CamelContextFactoryBean;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 *
 * @author yihtserns
 */
class CamelContextBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private Logger log = LoggerFactory.getLogger(getClass());
    private UnmarshallerParser dynamicEndpointParser = UnmarshallerParser.dynamicEndpointParser();
    Map<Class, Class> beanClass2ReplacementClass = new HashMap<Class, Class>();
    private Class<? extends CamelContextFactoryBean> customBeanClass;

    public CamelContextBeanDefinitionParser(Class<? extends CamelContextFactoryBean> customBeanClass) {
        this.customBeanClass = customBeanClass;
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        final String autoRegistererBeanName = "camel.autoRegister";
        if (!parserContext.getRegistry().containsBeanDefinition(autoRegistererBeanName)) {
            BeanDefinition autoRegistererDef = BeanDefinitionBuilder.genericBeanDefinition(ConsumerProducerTemplateAutoRegisterer.class).setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition();
            parserContext.registerBeanComponent(new BeanComponentDefinition(autoRegistererDef, autoRegistererBeanName));
        }
        String contextId = element.getAttribute("id");
        boolean implicitId = false;
        // lets avoid folks having to explicitly give an ID to a camel context
        if (ObjectHelper.isEmpty(contextId)) {
            // if no explicit id was set then use a default auto generated name
            CamelContextNameStrategy strategy = new DefaultCamelContextNameStrategy();
            contextId = strategy.getName();
            element.setAttributeNS(null, "id", contextId);
            implicitId = true;
        }
        AbstractBeanDefinition bd;
        try {
            bd = (AbstractBeanDefinition) Unmarshaller.INSTANCE.unmarshal(element, new CamelContextBeanHandler(element, customBeanClass, contextId, implicitId, parserContext));
            registerAllBeanDefs(removeBeanDefsPropertyFrom(bd, "beans"), parserContext);
            registerAllBeanDefs(removeBeanDefsPropertyFrom(bd, "endpoints"), parserContext);
            registerAllBeanDefs(removeBeanDefsPropertyFrom(bd, "redeliveryPolicies"), parserContext);
            registerAllBeanDefs(removeBeanDefsPropertyFrom(bd, "threadPools"), parserContext);
        } catch (Exception ex) {
            String msg = String.format("Unable to unmarshal <%s/>", element.getLocalName());
            parserContext.getReaderContext().fatal(msg, element, ex);
            return null;
        }
        // inject bean post processor so we can support @Produce etc.
        // no bean processor element so lets create it by our self
        String camelPostProcessorId = contextId + ":beanPostProcessor";
        BeanDefinition camelPostProcessor = // only register to camel context id as a String. Then we can look it up later
                // otherwise we get a circular reference in spring and it will not allow custom bean post processing
                // see more at CAMEL-1663
                BeanDefinitionBuilder.genericBeanDefinition(CamelBeanPostProcessor.class).addPropertyValue("camelId", contextId).setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition();
        parserContext.registerBeanComponent(new BeanComponentDefinition(camelPostProcessor, camelPostProcessorId));
        bd.getPropertyValues().addPropertyValue("beanPostProcessor", new RuntimeBeanReference(camelPostProcessorId));
        return bd;
    }

    private void registerAllBeanDefs(List<AbstractBeanDefinition> beanDefs, ParserContext parserContext) {
        for (AbstractBeanDefinition beanDef : beanDefs) {
            String id = (String) beanDef.getPropertyValues().get("id");
            parserContext.registerBeanComponent(new BeanComponentDefinition(beanDef, id));
        }
    }

    private List<AbstractBeanDefinition> removeBeanDefsPropertyFrom(BeanDefinition bd, String propertyName) {
        MutablePropertyValues pvs = bd.getPropertyValues();
        PropertyValue propertyValue = pvs.getPropertyValue(propertyName);
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        pvs.removePropertyValue(propertyName);
        return (List) propertyValue.getValue();
    }

    private class CamelContextBeanHandler implements BeanHandler<BeanDefinitionBuilder> {

        private final NamespacesCache namespacesCache = new NamespacesCache();
        private final Element camelContextElement;
        private final Class<? extends CamelContextFactoryBean> camelContextClass;
        private final String camelContextId;
        private final boolean implicitId;
        private final ParserContext parserContext;

        public CamelContextBeanHandler(Element camelContextElement, Class<? extends CamelContextFactoryBean> camelContextClass, String camelContextId, boolean implicitId, ParserContext parserContext) {
            this.camelContextElement = camelContextElement;
            this.camelContextClass = camelContextClass;
            this.camelContextId = camelContextId;
            this.implicitId = implicitId;
            this.parserContext = parserContext;
        }

        @Override
        public BeanDefinitionBuilder createBean(Class<?> beanClass, Element element) {
            if (beanClass2ReplacementClass.containsKey(beanClass)) {
                beanClass = beanClass2ReplacementClass.get(beanClass);
            }
            if (element == camelContextElement) {
                BeanDefinitionBuilder bean = SpringBeanHandler.INSTANCE.createBean(camelContextClass, element);
                bean.addPropertyValue("id", camelContextId);
                bean.addPropertyValue("implicitId", implicitId);
                return bean;
            }
            return SpringBeanHandler.INSTANCE.createBean(beanClass, element);
        }

        @Override
        public void setBeanProperty(BeanDefinitionBuilder bean, String propertyName, Object propertyValue) {
            if (propertyName.equals("uri") || propertyName.endsWith("Uri")) {
                String stringValue = (String) propertyValue;
                // remove all double spaces
                String changed = stringValue.replaceAll("\\s{2,}", "");
                if (!stringValue.equals(changed)) {
                    log.debug("Removed whitespace noise from attribute {} -> {}", stringValue, changed);
                    propertyValue = changed;
                }
            }
            if (propertyName.equals("dependsOn")) {
                String[] beanNames = StringUtils.tokenizeToStringArray((String) propertyValue, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
                bean.getRawBeanDefinition().setDependsOn(beanNames);
            } else {
                SpringBeanHandler.INSTANCE.setBeanProperty(bean, propertyName, propertyValue);
            }
        }

        @Override
        public AbstractBeanDefinition unmarshalWith(XmlAdapter xmlAdapter, Object from) throws Exception {
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
            if (CamelContextAware.class.isAssignableFrom(beanClass)) {
                bean.addPropertyReference("camelContext", camelContextId);
            }
            if (FromDefinition.class.isAssignableFrom(beanClass) || SendDefinition.class.isAssignableFrom(beanClass)) {
                registerEndpoint(element);
            }
            if (!customAttributes.isEmpty()) {
                Map<QName, String> qname2Value = new HashMap<QName, String>();
                for (Attr attribute : customAttributes) {
                    QName qname = new QName(attribute.getNamespaceURI(), attribute.getLocalName(), attribute.getPrefix());
                    qname2Value.put(qname, attribute.getValue());
                }
                bean.addPropertyValue("otherAttributes", qname2Value);
            }
            return SpringBeanHandler.INSTANCE.postProcess(bean, customAttributes, customElements);
        }

        private void registerEndpoint(Element element) {
            String id = element.getAttribute("id");
            // must have an id to be registered
            if (ObjectHelper.isNotEmpty(id)) {
                AbstractBeanDefinition definition = (AbstractBeanDefinition) dynamicEndpointParser.parse(element, parserContext);
                definition.getPropertyValues().addPropertyValue("camelContext", new RuntimeBeanReference(camelContextId));
                definition.setDependsOn(camelContextId);
                parserContext.registerBeanComponent(new BeanComponentDefinition(definition, id));
            }
        }
    }
}
