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
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.ServiceSupport;
import org.apache.camel.util.AsyncProcessorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by raul on 22/08/15.
 */
public class HystrixDelegateProcessor extends ServiceSupport implements AsyncProcessor {

    protected static final Logger LOG = LoggerFactory.getLogger(HystrixDelegateProcessor.class);

    /**
     * Hystrix common configuration.
     */
    private HystrixConfiguration configuration;

    /**
     * The target processor to invoke.
     */
    private Processor target;

    /**
     * Constructor.
     * @param target
     * @param config
     */
    public HystrixDelegateProcessor(Processor target, HystrixConfiguration config) {
        this.configuration = config;
        this.target = target;
    }

    /**
     * {@inheritDoc}
     * @param incoming
     * @param callback
     * @return
     */
    @Override
    public boolean process(final Exchange incoming, final AsyncCallback callback) {
        HystrixHelper.ensureRequestContextPresent(incoming, configuration);

        ProcessorInvokingObservableCommand command = new ProcessorInvokingObservableCommand(target, incoming, configuration);
        command.observe().subscribe(command.generateSubscriber(callback));

        return false;
    }

    /**
     * {@inheritDoc}
     * @param exchange
     * @throws Exception
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    protected void doStart() throws Exception {
        configuration.validateAndInit();
    }

    @Override
    protected void doStop() throws Exception {

    }
}
