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

import java.util.concurrent.atomic.AtomicBoolean;

import com.netflix.hystrix.HystrixObservableCommand;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.util.ExchangeHelper;
import rx.Observable;
import rx.Subscriber;

/**
 * {@link HystrixObservableCommand} that wraps the call to the target processor and applies a fallback if provided.
 */
public class ProcessorInvokingObservableCommand extends HystrixObservableCommand<Exchange> {

    /**
     * The {@link Exchange}.
     */
    private Exchange exchange;

    /**
     * The target processor/endpoint.
     */
    private Processor target;

    /**
     * The fallback processor/endpoint.
     */
    private Processor fallback;

    /**
     * The Hystrix configuration.
     */
    private HystrixConfiguration configuration;

    /**
     * Whether a subscriber has already been generated for this command or not.
     */
    private AtomicBoolean subscriberGenerated = new AtomicBoolean(false);

    /**
     * Constructor.
     * @param target
     * @param exchange
     * @param configuration
     */
    public ProcessorInvokingObservableCommand(Processor target, Exchange exchange, HystrixConfiguration configuration) {
        super(configuration.getSetter());
        this.exchange = exchange;
        this.target = target;
        this.fallback = configuration.getActualFallback();
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Observable<Exchange> construct() {
        return Observable.create(new CamelOnSubscribeFunction(target));
    }

    /**
     * Uses the <tt>fallback</tt> as a fallback if the user has set one.
     */
    @Override
    protected Observable<Exchange> resumeWithFallback() {
        if (fallback != null) {
            return Observable.create(new CamelOnSubscribeFunction(fallback));
        }
        return super.resumeWithFallback();
    }

    /**
     * Evaluates the cache key if the user has set a cache key expression.
     */
    @Override
    protected String getCacheKey() {
        return configuration.getCacheKeyExpression() == null ? super.getCacheKey()
                : configuration.getCacheKeyExpression().evaluate(exchange, String.class);
    }

    /**
     * @return The Exchange.
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * @return The HystrixConfiguration.
     */
    public HystrixConfiguration getConfiguration() {
        return configuration;
    }

    public Subscriber<Exchange> generateSubscriber(final AsyncCallback callback) {
        // control that only one subscriber can be generated for this command
        if (!subscriberGenerated.compareAndSet(false, true)) {
            throw new RuntimeCamelException("A Subscriber had already been generated for a Hystrix Command. Check your code for bugs.");
        }

        return new Subscriber<Exchange>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void onCompleted() {
                callback.done(false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onError(Throwable e) {
                exchange.setException(HystrixHelper.unwrapThrowable(e));
                callback.done(false);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onNext(Exchange result) {
                // do nothing, as the exchange would have been modified by the Processor already
                // unless this is a cached response in which case we can invoke the cacheMergeStrategy
                if (!ProcessorInvokingObservableCommand.this.isResponseFromCache()) {
                    return;
                }

                if (configuration.getCacheMergeStrategy() == null) {
                    ExchangeHelper.copyResults(exchange, result);
                } else {
                    configuration.getCacheMergeStrategy().call(exchange, result);
                }
            }
        };
    }

    private final class CamelOnSubscribeFunction implements Observable.OnSubscribe<Exchange> {

        private final Processor processor;

        private CamelOnSubscribeFunction(Processor processor) {
            this.processor = processor;
        }

        @Override
        public void call(Subscriber<? super Exchange> subscriber) {
            if (subscriber.isUnsubscribed()) {
                return;
            }

            try {
                // now let's invoke the processor; we don't care if this is an AsyncProcessor because at this
                // point we're already in a thread pool managed by Hystrix and there's no further optimisation
                // possible
                processor.process(exchange);

                // if the processor set an Exception on the exchange, let's notify the subscriber; we also
                // clear the exception from the Exchange because it will be set by the Subscriber
                if (exchange.getException() != null) {
                    Throwable t = exchange.getException();
                    exchange.setException(null);
                    subscriber.onError(HystrixHelper.maybeConvertThrowable(t, configuration.getExceptionsSuppressingFallback()));
                    return;
                }

                // else, we signal an OK and feed the same Exchange onto the Subscriber.
                // note: processors or the fallback processors have already modified the original Exchange, and
                // that's what Camel will see. However, we need to return a COPY of the Exchange to Hystrix for
                // caching purposes, as we don't want it to mutate between the time it was stored and the time
                // it's requested again.
                subscriber.onNext(exchange.copy());
                subscriber.onCompleted();
            } catch (Throwable t) {
                subscriber.onError(HystrixHelper.maybeConvertThrowable(t, configuration.getExceptionsSuppressingFallback()));
            }
        }
    }
}