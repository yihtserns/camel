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

import com.github.yihtserns.jaxbean.unmarshaller.JaxbeanUnmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.camel.model.Constants;
import org.apache.camel.spring.SpringModelJAXBContextFactory;

/**
 *
 * @author yihtserns
 */
public class Unmarshaller {

    public static final JaxbeanUnmarshaller INSTANCE;

    static {
        try {
            List<String> packages = new ArrayList<String>();
            packages.addAll(Arrays.asList(Constants.JAXB_CONTEXT_PACKAGES.split(":")));
            packages.addAll(Arrays.asList(SpringModelJAXBContextFactory.ADDITIONAL_JAXB_CONTEXT_PACKAGES.split(":")));

            List<Class<?>> jaxbClasses = new ArrayList<Class<?>>();
            for (String pkg : packages) {
                if (pkg.isEmpty()) {
                    continue;
                }

                String resourcePath = "/" + pkg.replace('.', '/') + "/jaxb.index";
                InputStream is = Unmarshaller.class.getResourceAsStream(resourcePath);
                if (is == null) {
                    throw new RuntimeException("Cannot find: " + resourcePath);
                }
                try {
                    Properties prop = new Properties();
                    prop.load(is);
                    for (Enumeration<?> classNames = prop.propertyNames(); classNames.hasMoreElements();) {
                        String className = (String) classNames.nextElement();
                        String fqcn = pkg + "." + className;
                        jaxbClasses.add(Class.forName(fqcn));
                    }
                } finally {
                    is.close();
                }
            }

            for (ListIterator<Class<?>> iter = jaxbClasses.listIterator(); iter.hasNext();) {
                Class<?> jaxbClass = iter.next();
                if (!jaxbClass.isAnnotationPresent(XmlRootElement.class)) {
                    iter.remove();
                }
            }

            INSTANCE = JaxbeanUnmarshaller.newInstance(jaxbClasses.toArray(new Class[jaxbClasses.size()]));
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void main(String[] args) {

    }
}
