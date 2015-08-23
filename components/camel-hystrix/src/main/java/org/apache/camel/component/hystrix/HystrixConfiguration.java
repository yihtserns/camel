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

import java.util.HashSet;
import java.util.Set;

import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.util.ExchangeHelper;
import rx.functions.Action2;

/**
 * Created by raul on 23/08/15.
 */
public class HystrixConfiguration {

    /**
     * Enumeration to indicate which type of fallback we want.
     */
    public enum FallbackType {

        /**
         * The fallback is a {@link Processor}. Use {@link #setFallbackProcessor(Processor)} to set it.
         */
        processor,

        /**
         * The fallback is an {@link Endpoint}. Use
         */
        endpoint,

        none
    }

    /**
     * A Hystrix {@link HystrixObservableCommand.Setter} setting the command group and command
     * key.
     */
    protected HystrixObservableCommand.Setter setter;

    /**
     * The fallback type.
     */
    protected FallbackType fallbackType = FallbackType.none;

    /**
     * A fallback processor (optional) to invoke in case the target fails.
     */
    protected Processor fallbackProcessor;

    /**
     * A fallback endpoint (optional) to invoke in case the target fails.
     */
    protected Endpoint fallbackEndpoint;

    /**
     * The actual fallback, cached.
     */
    protected Processor actualFallback;

    /**
     * An {@link Expression} (optional) to calculate a cache key in case the user wants to enable caching.
     */
    protected Expression cacheKeyExpression;

    /**
     * A strategy for merging responses from the cache. If not set, Camel will use
     * {@link ExchangeHelper#copyResults(Exchange, Exchange)} to copy the cached {@link Exchange} into the incoming
     * <tt>Exchange</tt>, which will override the body, message headers and Exchange properties.
     * <p>
     * Setting a custom merge strategy allows the user to decide exactly what information has to be copied over.
     * <p>
     * The custom merge strategy is an {@link Action2} function that takes the incoming original <tt>Exchange</tt> as
     * its first argument and the cached <tt>Exchange</tt> as the second argument.
     */
    protected Action2<Exchange, Exchange> cacheMergeStrategy;

    /**
     * Whether or not to propagate the {@link HystrixRequestContext}.
     */
    protected boolean propagateRequestContext = true;

    /**
     * Set of exceptions to ignore and wrap in a {@link com.netflix.hystrix.exception.HystrixBadRequestException}, so
     * that they do not trigger a fallback.
     */
    protected Set<Class<? extends Throwable>> exceptionsSuppressingFallback = new HashSet<>();

    /**
     * Constructor.
     * @param
     */
    public HystrixConfiguration() {

    }

    /**
     * Validates that the provided options are correct and coherent.
     * @throws IllegalArgumentException if options are invalid or incoherent.
     */
    public void validateAndInit() throws Exception {
        if (fallbackType == FallbackType.endpoint && fallbackEndpoint == null) {
            throw new IllegalArgumentException("Fallback type is 'endpoint' but no fallback endpoint was provided");
        }

        if (fallbackType == FallbackType.processor && fallbackProcessor == null) {
            throw new IllegalArgumentException("Fallback type is 'processor' but no fallback processor was provided");
        }

        if (fallbackType == FallbackType.none && (fallbackEndpoint != null || fallbackProcessor != null)) {
            throw new IllegalArgumentException("Fallback type is 'none' but a fallback processor or endpoint was provided");
        }

        if (fallbackType == FallbackType.none) {
            actualFallback = null;
        }

        if (fallbackType == FallbackType.endpoint) {
            actualFallback = fallbackEndpoint.createProducer();
        } else if (fallbackType == FallbackType.processor) {
            actualFallback = fallbackProcessor;
        }
    }

    /**
     * @return The Expression that evaluates the cache key, if any. Else, <tt>null</tt>.
     */
    public Expression getCacheKeyExpression() {
        return cacheKeyExpression;
    }

    /**
     * Sets an {@link Expression} to extract a cache key from the incoming {@link Exchange}. It must evaluate to a
     * <tt>String</tt>. If set, Hystrix will enable response caching.
     *
     * @param cacheKeyExpression
     */
    public void setCacheKeyExpression(Expression cacheKeyExpression) {
        this.cacheKeyExpression = cacheKeyExpression;
    }

    /**
     * @return The current cache merge strategy, if any.
     */
    public Action2<Exchange, Exchange> getCacheMergeStrategy() {
        return cacheMergeStrategy;
    }

    /**
     * A strategy for merging responses from the cache. If not set, Camel will use
     * {@link ExchangeHelper#copyResults(Exchange, Exchange)} to copy the cached {@link Exchange} into the incoming
     * <tt>Exchange</tt>, which will override the body, message headers and Exchange properties.
     * <p>
     * Setting a custom merge strategy allows the user to decide exactly what information has to be copied over.
     * <p>
     * The custom merge strategy is an {@link Action2} function that takes the incoming original <tt>Exchange</tt> as
     * its first argument and the cached <tt>Exchange</tt> as the second argument.

     * @param cacheMergeStrategy
     */
    public void setCacheMergeStrategy(Action2<Exchange, Exchange> cacheMergeStrategy) {
        this.cacheMergeStrategy = cacheMergeStrategy;
    }

    /**
     * @return The current {@link HystrixRequestContext} propagation policy.
     */
    public boolean isPropagateRequestContext() {
        return propagateRequestContext;
    }

    /**
     * Sets whether or not to propagate the {@link HystrixRequestContext} from other threads via a message header. By
     * default the value is <tt>true</tt> (enabled).
     * @param propagateRequestContext
     */
    public void setPropagateRequestContext(boolean propagateRequestContext) {
        this.propagateRequestContext = propagateRequestContext;
    }

    /**
     * @return The Setter.
     */
    public HystrixObservableCommand.Setter getSetter() {
        return setter;
    }

    /**
     * Hystrix Setter for HystrixObservableCommand properties.
     * @param setter
     */
    public void setSetter(HystrixObservableCommand.Setter setter) {
        this.setter = setter;
    }

    /**
     * @return Ignored exceptions.
     */
    public Set<Class<? extends Throwable>> getExceptionsSuppressingFallback() {
        return exceptionsSuppressingFallback;
    }

    /**
     * Sets the exceptions to ignore and wrap in a {@link com.netflix.hystrix.exception.HystrixBadRequestException}, so
     * that fallbacks are bypassed.
     *
     * @param exceptionsSuppressingFallback
     */
    public void setExceptionsSuppressingFallback(Set<Class<? extends Throwable>> exceptionsSuppressingFallback) {
        this.exceptionsSuppressingFallback = exceptionsSuppressingFallback;
    }

    /**
     * @return The fallback processor, if one was set. Else, <tt>null</tt>.
     */
    public Processor getFallbackProcessor() {
        return fallbackProcessor;
    }

    /**
     * Sets the fallback processor (optional). If a fallback processor is set, Hystrix will call it when the target
     * processor returns an Exception.
     *
     * @param fallbackProcessor
     */
    public void setFallbackProcessor(Processor fallbackProcessor) {
        this.fallbackProcessor = fallbackProcessor;
    }

    /**
     * @return The fallback type. By default,
     * {@link org.apache.camel.component.hystrix.HystrixConfiguration.FallbackType#none}.
     */
    public FallbackType getFallbackType() {
        return fallbackType;
    }

    /**
     * Sets the fallback type. By default,
     * {@link org.apache.camel.component.hystrix.HystrixConfiguration.FallbackType#none}.
     * <p>
     * Camel will validate if the {@link Processor} or {@link Endpoint} has been provided; else it will throw
     *
     * @param fallbackType
     */
    public void setFallbackType(FallbackType fallbackType) {
        this.fallbackType = fallbackType;
    }

    /**
     * @return
     */
    public Endpoint getFallbackEndpoint() {
        return fallbackEndpoint;
    }

    /**
     * @param fallbackEndpoint
     */
    public void setFallbackEndpoint(Endpoint fallbackEndpoint) {
        this.fallbackEndpoint = fallbackEndpoint;
    }

    public Processor getActualFallback() {
        return actualFallback;
    }

}
