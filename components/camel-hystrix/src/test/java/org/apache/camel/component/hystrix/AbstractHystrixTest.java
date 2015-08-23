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

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.test.junit4.CamelTestSupport;

/**
 * Abstract Hystrix test.
 */
public abstract class AbstractHystrixTest extends CamelTestSupport {

    protected HystrixComponent hystrix;

    protected HystrixObservableCommand.Setter setter = HystrixObservableCommand.Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey("cameltest"))
            .andCommandKey(HystrixCommandKey.Factory.asKey("cameltest"));

    protected Processor target = new Processor() {
        int i;
        @Override
        public void process(Exchange exchange) throws Exception {
            exchange.getIn().setBody("Hello World " + i++);
        }
    };

    protected Processor fallback = new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
            exchange.getIn().setBody("Hello Fallback");
        }
    };

    @Override
    protected void doPostSetup() throws Exception {
        hystrix = context.getComponent("hystrix", HystrixComponent.class);
    }

}