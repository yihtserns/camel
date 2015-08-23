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
package org.apache.camel.component.hystrix.builders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.netflix.hystrix.HystrixObservableCommand;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.component.hystrix.HystrixComponent;
import org.apache.camel.component.hystrix.HystrixConfiguration;
import org.apache.camel.component.hystrix.HystrixDelegateEndpoint;
import org.apache.camel.component.hystrix.HystrixDelegateProcessor;
import org.apache.camel.util.CamelContextHelper;
import rx.functions.Action2;

/**
 * Fluent DSL for building Hystrix Wrappers around {@link Processor}s, {@link Endpoint}s, etc.
 * <p>
 * The starting point to construct a wrapper is via {@link HystrixComponent#wrapper()}. Do not use the constructor
 * of this class.
 *
 */
public final class HystrixWrappers {

    private CamelContext context;

    public HystrixWrappers(CamelContext context) {
        this.context = context;
    }

    public HystrixStaticEndpointWrapperBuilder forStaticEndpoint(String uri, HystrixObservableCommand.Setter setter) {
        Endpoint endpoint = CamelContextHelper.getMandatoryEndpoint(context, uri);
        return forStaticEndpoint(endpoint, setter);
    }

    public HystrixStaticEndpointWrapperBuilder forStaticEndpoint(Endpoint endpoint, HystrixObservableCommand.Setter setter) {
        HystrixStaticEndpointWrapperBuilder answer = new HystrixStaticEndpointWrapperBuilder(endpoint);
        answer.configuration.setSetter(setter);
        return answer;
    }

    public HystrixProcessorWrapperBuilder forProcessor(Processor processor, HystrixObservableCommand.Setter setter) {
        HystrixProcessorWrapperBuilder answer = new HystrixProcessorWrapperBuilder(processor);
        answer.configuration.setSetter(setter);
        return answer;
    }

    public abstract class AbstractHystrixWrapperBuilder<T extends AbstractHystrixWrapperBuilder> {

        protected HystrixConfiguration configuration = new HystrixConfiguration();

        public abstract T thisBuilder();

        public T withCacheKey(Expression expression) {
            configuration.setCacheKeyExpression(expression);
            return thisBuilder();
        }

        public T withCacheMergeStrategy(Action2<Exchange, Exchange> strategy) {
            configuration.setCacheMergeStrategy(strategy);
            return thisBuilder();
        }

        public T withPropagateRequestContext(boolean propagate) {
            configuration.setPropagateRequestContext(propagate);
            return thisBuilder();
        }

        public T withFallbackProcessor(Processor fallback) {
            configuration.setFallbackType(HystrixConfiguration.FallbackType.processor);
            configuration.setFallbackProcessor(fallback);
            return thisBuilder();
        }

        public T withFallbackEndpoint(Endpoint fallback) {
            configuration.setFallbackType(HystrixConfiguration.FallbackType.endpoint);
            configuration.setFallbackEndpoint(fallback);
            return thisBuilder();
        }

        public T withSuppressedExceptions(Class<? extends Throwable>... throwables) {
            Set<Class<? extends Throwable>> t = new HashSet<>(Arrays.asList(throwables));
            configuration.setExceptionsSuppressingFallback(t);
            return thisBuilder();
        }

    }

    public class HystrixStaticEndpointWrapperBuilder extends AbstractHystrixWrapperBuilder<HystrixStaticEndpointWrapperBuilder> {

        private HystrixDelegateEndpoint answer;

        public HystrixStaticEndpointWrapperBuilder(Endpoint target) {
            this.answer = new HystrixDelegateEndpoint(target, configuration);
        }

        public HystrixDelegateEndpoint build() {
            return answer;
        }

        @Override
        public HystrixStaticEndpointWrapperBuilder thisBuilder() {
            return this;
        }

    }

    public class HystrixProcessorWrapperBuilder extends AbstractHystrixWrapperBuilder<HystrixProcessorWrapperBuilder> {

        private HystrixDelegateProcessor answer;

        public HystrixProcessorWrapperBuilder(Processor processor) {
            answer = new HystrixDelegateProcessor(processor, configuration);
        }

        public HystrixDelegateProcessor build() {
            return answer;
        }

        @Override
        public HystrixProcessorWrapperBuilder thisBuilder() {
            return this;
        }

    }
}
