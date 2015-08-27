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

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.util.ServiceHelper;
import org.junit.Test;
import rx.functions.Action2;

/**
 * Tests for Processors.
 */
public class HystrixProcessorTest extends AbstractHystrixTest {

    @Test
    public void testProcessorOk() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(target, setter)
                .build();

        Exchange exchange = new DefaultExchange(context);
        hystrixP.process(exchange);
        assertEquals("Hello World 0", exchange.getIn().getBody());
    }

    @Test
    public void testProcessorFallbackExceptionThrown() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        throw new CamelException("Bang!");
                    }
                }, setter)
                .withFallbackProcessor(fallback).build();

        ServiceHelper.startService(hystrixP);

        Exchange exchange = new DefaultExchange(context);
        hystrixP.process(exchange);
        assertEquals("Hello Fallback", exchange.getIn().getBody());
    }

    @Test
    public void testProcessorFallbackExceptionSet() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.setException(new CamelException("Bang!"));
                    }
                }, setter)
                .withFallbackProcessor(fallback).build();

        ServiceHelper.startService(hystrixP);

        Exchange exchange = new DefaultExchange(context);
        hystrixP.process(exchange);
        assertEquals("Hello Fallback", exchange.getIn().getBody());
    }

    @Test
    public void testProcessorNoFallbackSuppressedExceptionThrown() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        throw new DummyException("Bang!");
                    }
                }, setter)
                .withFallbackProcessor(fallback)
                .suppressFallbackForExceptions(DummyException.class)
                .build();

        ServiceHelper.startService(hystrixP);

        Exchange exchange = new DefaultExchange(context);
        hystrixP.process(exchange);

        // assert fallback not invoked and exception present and unwrapped
        assertNotEquals("Hello Fallback", exchange.getIn().getBody());
        assertEquals(DummyException.class, exchange.getException().getClass());
    }

    @Test
    public void testProcessorNoFallbackSuppressedExceptionSet() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.setException(new DummyException("Bang!"));
                    }
                }, setter)
                .withFallbackProcessor(fallback)
                .suppressFallbackForExceptions(DummyException.class)
                .build();

        ServiceHelper.startService(hystrixP);

        Exchange exchange = new DefaultExchange(context);
        hystrixP.process(exchange);

        // assert fallback not invoked and exception present and unwrapped
        assertNotEquals("Hello Fallback", exchange.getIn().getBody());
        assertEquals(DummyException.class, exchange.getException().getClass());
    }

    @Test
    public void testProcessorWithCache() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(target, setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .build();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello");
        hystrixP.process(exchange);
        exchange.getIn().setBody("Hello");
        hystrixP.process(exchange);

        assertEquals("Hello World 0", exchange.getIn().getBody());
    }

    @Test
    public void testProcessorWithCacheAndCustomMergeFunction() throws Exception {
        HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(target, setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .withCacheMergeStrategy(new Action2<Exchange, Exchange>() {
                    @Override
                    public void call(Exchange incoming, Exchange result) {
                        incoming.getIn().setBody("MERGED: " + result.getIn().getBody(String.class));
                    }
                })
                .build();

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello");
        hystrixP.process(exchange);
        exchange.getIn().setBody("Hello");
        hystrixP.process(exchange);

        assertEquals("MERGED: Hello World 0", exchange.getIn().getBody());
    }

    @Test
    public void testProcessorWithNoRequestPropagation() throws Exception {
        final HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(target, setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .withPropagateRequestContext(false)
                .build();

        final Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello");
        hystrixP.process(exchange);

        // now run the second request in a different thread
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                exchange.getIn().setBody("Hello");
                try {
                    hystrixP.process(exchange);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Hello World 1", exchange.getIn().getBody());
    }

    @Test
    public void testProcessorWithRequestPropagation() throws Exception {
        final HystrixDelegateProcessor hystrixP = hystrix.wrapper()
                .forProcessor(target, setter)
                .withCacheKey(ExpressionBuilder.bodyExpression())
                .withPropagateRequestContext(true)
                .build();

        final Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello");
        hystrixP.process(exchange);

        // now run the second request in a different thread
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                exchange.getIn().setBody("Hello");
                try {
                    hystrixP.process(exchange);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Hello World 0", exchange.getIn().getBody());
    }

}
