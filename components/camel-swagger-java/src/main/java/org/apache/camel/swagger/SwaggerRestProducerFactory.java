/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.swagger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.NoSuchBeanException;
import org.apache.camel.Producer;
import org.apache.camel.spi.RestProducerFactory;
import org.apache.camel.util.CollectionStringBuffer;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.util.ResourceHelper.resolveMandatoryResourceAsInputStream;

public class SwaggerRestProducerFactory implements RestProducerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerRestProducerFactory.class);

    @Override
    public Producer createProducer(CamelContext camelContext, String host,
                                   String verb, String basePath, String uriTemplate,
                                   String consumes, String produces, Map<String, Object> parameters) throws Exception {

        String apiDoc = (String) parameters.get("apiDoc");
        // load json model
        if (apiDoc == null) {
            throw new IllegalArgumentException("Swagger api-doc must be configured using the apiDoc option");
        }

        String path = uriTemplate != null ? uriTemplate : basePath;
        // path must start with a leading slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        Swagger swagger = loadSwaggerModel(camelContext, apiDoc);
        Operation operation = getSwaggerOperation(swagger, verb, path);
        if (operation == null) {
            throw new IllegalArgumentException("Swagger api-doc does not contain operation for " + verb + ":" + path);
        }

        String componentName = (String) parameters.get("componentName");

        Producer producer = createHttpProducer(camelContext, swagger, operation, host, verb, path, produces, consumes, componentName, parameters);
        return producer;
    }

    private Swagger loadSwaggerModel(CamelContext camelContext, String apiDoc) throws Exception {
        InputStream is = resolveMandatoryResourceAsInputStream(camelContext, apiDoc);
        try {
            SwaggerParser parser = new SwaggerParser();
            String json = camelContext.getTypeConverter().mandatoryConvertTo(String.class, is);
            LOG.debug("Loaded swagger api-doc:\n{}", json);
            return parser.parse(json);
        } finally {
            IOHelper.close(is);
        }
    }

    private Operation getSwaggerOperation(Swagger swagger, String verb, String path) {
        Path modelPath = swagger.getPath(path);
        if (modelPath == null) {
            return null;
        }

        // get,put,post,head,delete,patch,options
        Operation op = null;
        if ("get".equals(verb)) {
            op = modelPath.getGet();
        } else if ("put".equals(verb)) {
            op = modelPath.getPut();
        } else if ("post".equals(verb)) {
            op = modelPath.getPost();
        } else if ("head".equals(verb)) {
            op = modelPath.getHead();
        } else if ("delete".equals(verb)) {
            op = modelPath.getDelete();
        } else if ("patch".equals(verb)) {
            op = modelPath.getPatch();
        } else if ("options".equals(verb)) {
            op = modelPath.getOptions();
        }
        return op;
    }

    private Producer createHttpProducer(CamelContext camelContext, Swagger swagger, Operation operation,
                                        String host, String verb, String path, String consumes, String produces,
                                        String componentName, Map<String, Object> parameters) throws Exception {

        LOG.debug("Using Swagger operation: {} with {} {}", operation, verb, path);

        RestProducerFactory factory = null;
        String cname = null;
        if (componentName != null) {
            Object comp = camelContext.getRegistry().lookupByName(componentName);
            if (comp != null && comp instanceof RestProducerFactory) {
                factory = (RestProducerFactory) comp;
            } else {
                comp = camelContext.getComponent(componentName);
                if (comp != null && comp instanceof RestProducerFactory) {
                    factory = (RestProducerFactory) comp;
                }
            }

            if (factory == null) {
                if (comp != null) {
                    throw new IllegalArgumentException("Component " + componentName + " is not a RestProducerFactory");
                } else {
                    throw new NoSuchBeanException(componentName, RestProducerFactory.class.getName());
                }
            }
            cname = componentName;
        }

        // try all components
        if (factory == null) {
            for (String name : camelContext.getComponentNames()) {
                Component comp = camelContext.getComponent(name);
                if (comp != null && comp instanceof RestProducerFactory) {
                    factory = (RestProducerFactory) comp;
                    cname = name;
                    break;
                }
            }
        }

        // lookup in registry
        if (factory == null) {
            Set<RestProducerFactory> factories = camelContext.getRegistry().findByType(RestProducerFactory.class);
            if (factories != null && factories.size() == 1) {
                factory = factories.iterator().next();
            }
        }

        if (factory != null) {
            LOG.debug("Using RestProducerFactory: {}", factory);

            if (produces == null) {
                CollectionStringBuffer csb = new CollectionStringBuffer(",");
                List<String> list = operation.getProduces();
                if (list == null) {
                    list = swagger.getProduces();
                }
                if (list != null) {
                    for (String s : list) {
                        csb.append(s);
                    }
                }
                produces = csb.isEmpty() ? null : csb.toString();
            }
            if (consumes == null) {
                CollectionStringBuffer csb = new CollectionStringBuffer(",");
                List<String> list = operation.getConsumes();
                if (list == null) {
                    list = swagger.getConsumes();
                }
                if (list != null) {
                    for (String s : list) {
                        csb.append(s);
                    }
                }
                consumes = csb.isEmpty() ? null : csb.toString();
            }

            String basePath;
            String uriTemplate;
            if (host == null) {
                // if no explicit host has been configured then use host and base path from the swagger api-doc
                host = swagger.getHost();
                basePath = swagger.getBasePath();
                uriTemplate = path;
            } else {
                // path includes also uri template
                basePath = path;
                uriTemplate = null;
            }

            return factory.createProducer(camelContext, host, verb, basePath, uriTemplate, consumes, produces, parameters);

        } else {
            throw new IllegalStateException("Cannot find RestProducerFactory in Registry or as a Component to use");
        }
    }
}