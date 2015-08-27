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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.util.ObjectHelper;
import org.junit.Test;
import rx.functions.Action2;

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

    @Test
    public void testEndpointFallbackExceptionThrown() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withFallbackProcessor(fallback)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new DummyException("Bang!");
            }
        });
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        assertEquals("Hello Fallback", template.requestBody(hystrixE, "Hello World"));

    }

    @Test
    public void testEndpointFallbackExceptionSet() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withFallbackProcessor(fallback)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setException(new DummyException("Bang!"));
            }
        });
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        assertEquals("Hello Fallback", template.requestBody(hystrixE, "Hello World"));
    }

    @Test
    public void testEndpointNoFallbackSuppressedExceptionThrown() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withFallbackProcessor(fallback)
                .suppressFallbackForExceptions(DummyException.class)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new DummyException("Bang!");
            }
        });
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        // producer template will throw the exception, as it's suppressed for fallback
        try {
            template.requestBody(hystrixE, "Hello World");
        } catch (CamelExecutionException e) {
            assertEquals(DummyException.class, ObjectHelper.getException(DummyException.class, e).getClass());
            return;
        }

        fail("Should have thrown the exception, as fallback is suppressed");
    }

    @Test
    public void testEndpointNoFallbackSuppressedExceptionSet() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withFallbackProcessor(fallback)
                .suppressFallbackForExceptions(DummyException.class)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setException(new DummyException("Bang!"));
            }
        });
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        // producer template will throw the exception, as it's suppressed for fallback
        try {
            template.requestBody(hystrixE, "Hello World");
        } catch (CamelExecutionException e) {
            assertEquals(DummyException.class, ObjectHelper.getException(DummyException.class, e).getClass());
            return;
        }

        fail("Should have thrown the exception, as fallback is suppressed");
    }

    @Test
    public void testEndpointWithAndWithoutCache() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(target);
        getMockEndpoint("mock:test").expectedMessageCount(1);
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        assertEquals("Hello World 0", template.requestBody(hystrixE, "Hello World"));
        assertEquals("Hello World 0", template.requestBody(hystrixE, "Hello World"));

        assertMockEndpointsSatisfied();

        // now remove the cache from that endpoint, and we should receive different responses now
        getMockEndpoint("mock:test").expectedMessageCount(3);

        hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .build();
        hystrixE.setCamelContext(context);

        assertEquals("Hello World 1", template.requestBody(hystrixE, "Hello World"));
        assertEquals("Hello World 2", template.requestBody(hystrixE, "Hello World"));

        assertMockEndpointsSatisfied();

    }

    @Test
    public void testEndpointWithCacheAndCustomMergeFunction() throws Exception {
        HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .withCacheMergeStrategy(new Action2<Exchange, Exchange>() {
                    @Override
                    public void call(Exchange incoming, Exchange result) {
                        incoming.getIn().setBody("MERGED: " + result.getIn().getBody(String.class));
                    }
                })
                .build();

        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(target);
        getMockEndpoint("mock:test").expectedMessageCount(1);
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");

        assertEquals("Hello World 0", template.requestBody(hystrixE, "Hello World"));
        assertEquals("MERGED: Hello World 0", template.requestBody(hystrixE, "Hello World"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEndpointWithNoRequestPropagation() throws Exception {
        final HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .withPropagateRequestContext(false)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(target);
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");
        getMockEndpoint("mock:test").message(1).body().isEqualTo("Hello World");

        final HystrixRequestContext context = HystrixRequestContext.initializeContext();
        assertEquals("Hello World 0", template.requestBodyAndHeader(hystrixE, "Hello World",
                HystrixComponent.HYSTRIX_REQUEST_CONTEXT_HEADER_NAME, context));

        // now run the second request in a different thread with the same request context
        // ensure that cache is not used, because request context was not propagated
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertEquals("Hello World 1", template.requestBodyAndHeader(hystrixE, "Hello World",
                        HystrixComponent.HYSTRIX_REQUEST_CONTEXT_HEADER_NAME, context));
                latch.countDown();
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testEndpointWithRequestPropagation() throws Exception {
        final HystrixDelegateEndpoint hystrixE = hystrix.wrapper()
                .forStaticEndpoint("mock:test", setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .withPropagateRequestContext(true)
                .build();
        hystrixE.setCamelContext(context);

        getMockEndpoint("mock:test").whenAnyExchangeReceived(target);
        getMockEndpoint("mock:test").message(0).body().isEqualTo("Hello World");
        getMockEndpoint("mock:test").message(1).body().isEqualTo("Hello World");

        final HystrixRequestContext context = HystrixRequestContext.initializeContext();
        assertEquals("Hello World 0", template.requestBodyAndHeader(hystrixE, "Hello World",
                HystrixComponent.HYSTRIX_REQUEST_CONTEXT_HEADER_NAME, context));

        // now run the second request in a different thread with the same request context
        // ensure that cache IS used; response should be the same as the initial one
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertEquals("Hello World 0", template.requestBodyAndHeader(hystrixE, "Hello World",
                        HystrixComponent.HYSTRIX_REQUEST_CONTEXT_HEADER_NAME, context));
                latch.countDown();
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
    }

}
