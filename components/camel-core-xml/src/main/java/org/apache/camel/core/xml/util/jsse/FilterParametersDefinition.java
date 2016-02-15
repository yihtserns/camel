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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.camel.CamelContext;
import org.apache.camel.util.jsse.FilterParameters;

/**
 * Represents a set of regular expression based filter patterns for
 * including and excluding content of some type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filterParameters", propOrder = {"include", "exclude"})
public class FilterParametersDefinition {

    @XmlElement
    protected List<String> include;
    @XmlElement
    protected List<String> exclude;

    public FilterParameters create(CamelContext camelContext) {
        FilterParameters instance = new FilterParameters();
        instance.getInclude().addAll(include);
        instance.getExclude().addAll(exclude);
        instance.setCamelContext(camelContext);

        return instance;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }
}
