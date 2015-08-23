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

import org.junit.Test;

/**
 * Tests for Hystrix endpoints.
 */
public class HystrixEndpointTest extends AbstractHystrixTest {

    @Test
    public void testEndpointOk() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(target);
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        assertEquals("Hello World 0", template.requestBody(hystrixE, "Hello World"));

        assertMockEndpointsSatisfied();

    }

}
