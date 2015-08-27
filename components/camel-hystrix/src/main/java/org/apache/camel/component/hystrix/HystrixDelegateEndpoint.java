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
package org.apache.camel.component.hystrix;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.util.ObjectHelper;

/**
 * Created by raul on 16/08/15.
 */
public class HystrixDelegateEndpoint extends DefaultEndpoint {

    /**
     * Hystrix common configuration.
     */
    private final HystrixConfiguration configuration;

    /**
     * Target endpoint to invoke.
     */
    private Endpoint target;

    /**
     * Target endpoint to invoke.
     */
    private Producer producer;

    public HystrixDelegateEndpoint(Endpoint target, HystrixConfiguration configuration) {
        this.target = target;
        this.configuration = configuration;
    }

    @Override
    public Producer createProducer() throws Exception {
        ObjectHelper.notNull(target, "target endpoint");
        producer = target.createProducer();
        return new HystrixDelegateProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("The camel-hystrix component does not allow consumers yet.");
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected String createEndpointUri() {
        return "dummy";
    }

    public Endpoint getTarget() {
        return target;
    }

    public Producer getProducer() {
        return producer;
    }

    public HystrixConfiguration getConfiguration() {
        return configuration;
    }
}
