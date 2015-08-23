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

import org.apache.camel.AsyncCallback;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by raul on 24/08/15.
 */
public class HystrixDelegateProducer extends DefaultAsyncProducer {

    protected static final Logger LOG = LoggerFactory.getLogger(HystrixDelegateProducer.class);

    private final HystrixDelegateEndpoint endpoint;

    private final HystrixConfiguration configuration;

    public HystrixDelegateProducer(Endpoint endpoint) {
        super(endpoint);
        this.endpoint = (HystrixDelegateEndpoint) endpoint;
        this.configuration = this.endpoint.getConfiguration();
    }

    @Override
    public boolean process(Exchange incoming, AsyncCallback callback) {
        HystrixHelper.ensureRequestContextPresent(incoming, configuration);

        ProcessorInvokingObservableCommand command = new ProcessorInvokingObservableCommand(endpoint.getProducer(), incoming, configuration);
        command.observe().subscribe(command.generateSubscriber(callback));

        return false;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        endpoint.getConfiguration().validateAndInit();
    }
}
