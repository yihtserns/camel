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

import java.util.Set;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Hystrix helpers.
 */
public final class HystrixHelper {

    private HystrixHelper() { }

    /**
     * Creates a {@link Processor} to initialize a new {@link HystrixRequestContext}.
     * 
     * @param force Whether to initialize a new context even if one already exists.
     * @return The resulting processor.
     */
    public static Processor initializeContext(final boolean force) {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                if (!HystrixRequestContext.isCurrentThreadInitialized() || force) {
                    HystrixRequestContext.initializeContext();
                }
            }
        };
    }

    /**
     * Either initializes a new {@link HystrixRequestContext}, or, if allowed by {@link HystrixConfiguration#isPropagateRequestContext()},
     * injects an existent HystrixRequestContext from the Camel {@link HystrixComponent#HYSTRIX_REQUEST_CONTEXT_HEADER_NAME} message header.
     *
     * If propagation is enabled and a new context is initialized, it is injected in the above mentioned header for subsequent
     * propagation if necessary.
     *
     * This allows the context to be propagated across threads within the same Exchange.
     */
    public static void ensureRequestContextPresent(Exchange incoming, HystrixConfiguration configuration) {
        if (HystrixRequestContext.getContextForCurrentThread() != null) {
            return;
        }

        if (configuration.isPropagateRequestContext()) {
            HystrixRequestContext hRq = incoming.getIn().getHeader(HystrixComponent.HYSTRIX_REQUEST_CONTEXT_HEADER_NAME, HystrixRequestContext.class);
            if (hRq != null) {
                HystrixRequestContext.setContextOnCurrentThread(hRq);
            } else {
                hRq = HystrixRequestContext.initializeContext();
                incoming.getIn().setHeader(HystrixComponent.HYSTRIX_REQUEST_CONTEXT_HEADER_NAME, hRq);
            }
        } else {
            HystrixRequestContext.initializeContext();
        }
    }

    public static Throwable maybeConvertThrowable(Throwable t, Set<Class<? extends Throwable>> suppressed) {
        return suppressed.contains(t.getClass()) ? new HystrixBadRequestException("Exception suppressed for fallback", t) : t;
    }

    public static Throwable unwrapThrowable(Throwable t) {
        return t instanceof HystrixBadRequestException ? t.getCause() : t;
    }

}
