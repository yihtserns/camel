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

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.spring.CamelConsumerTemplateFactoryBean;
import org.apache.camel.spring.CamelContextFactoryBean;
import org.apache.camel.spring.CamelProducerTemplateFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 *
 * @author yihtserns
 */
public class ConsumerProducerTemplateAutoRegisterer implements BeanDefinitionRegistryPostProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        List<AbstractBeanDefinition> camelContextDefs = new ArrayList<AbstractBeanDefinition>();
        boolean hasProducerTemplate = false;
        boolean hasConsumerTemplate = false;
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition bd = registry.getBeanDefinition(beanName);
            if (!(bd instanceof AbstractBeanDefinition)) {
                continue;
            }
            AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
            if (!abd.hasBeanClass()) {
                continue;
            }
            Class<?> beanClass = abd.getBeanClass();
            hasProducerTemplate |= CamelProducerTemplateFactoryBean.class.isAssignableFrom(beanClass);
            hasConsumerTemplate |= CamelConsumerTemplateFactoryBean.class.isAssignableFrom(beanClass);

            if (CamelContextFactoryBean.class.isAssignableFrom(beanClass)) {
                camelContextDefs.add(abd);
            }
        }

        if (camelContextDefs.size() == 1) {
            AbstractBeanDefinition camelContextDef = camelContextDefs.get(0);
            String camelContextId = (String) camelContextDef.getPropertyValues().get("id");

            if (!hasProducerTemplate) {
                String id = "camel.default.producerTemplate";
                Class<?> beanClass = CamelProducerTemplateFactoryBean.class;
                tryRegister(beanClass, camelContextId, registry, id);
            }
            if (!hasConsumerTemplate) {
                String id = "camel.default.consumerTemplate";
                Class<?> beanClass = CamelConsumerTemplateFactoryBean.class;
                tryRegister(beanClass, camelContextId, registry, id);
            }
        }
    }

    private void tryRegister(Class<?> beanClass, String camelContextId, BeanDefinitionRegistry registry, String id) throws BeanDefinitionStoreException {
        BeanDefinition defaultBeanDef = BeanDefinitionBuilder.genericBeanDefinition(beanClass)
                .addPropertyReference("camelContext", camelContextId)
                .getBeanDefinition();
        registry.registerBeanDefinition(id, defaultBeanDef);
        if (log.isDebugEnabled()) {
            log.debug("Registered default: {} with id: {} on camel context: {}",
                    new Object[]{beanClass, id, camelContextId});
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
