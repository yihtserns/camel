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

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.builder.xml.Namespaces;
import org.w3c.dom.Element;

/**
 * @author yihtserns
 */
final class NamespacesCache {

    private Map<Element, Namespaces> element2Namespaces = new HashMap<Element, Namespaces>();

    public Namespaces getOrGenerateNamespaces(Element element) {
        Namespaces namespaces = element2Namespaces.get(element);
        if (namespaces == null) {
            namespaces = new Namespaces(element);
            element2Namespaces.put(element, namespaces);
        }
        return namespaces;
    }
}
