/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.blueskykong.gateway.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.tuple.Tuple;

import java.util.concurrent.TimeUnit;


/**
 * Sample throttling filter.
 * See https://github.com/bbeck/token-bucket
 */
public class ThrottleGatewayFilterFactory implements GatewayFilterFactory {
	private Log log = LogFactory.getLog(getClass());

	@Override
	public GatewayFilter apply(Tuple args) {
		int capacity = args.getInt("capacity");
		int refillTokens = args.getInt("refillTokens");
		int refillPeriod = args.getInt("refillPeriod");
		TimeUnit refillUnit = TimeUnit.valueOf(args.getString("refillUnit"));
		return apply(capacity, refillTokens, refillPeriod, refillUnit);
	}

	public GatewayFilter apply(int capacity, int refillTokens, int refillPeriod, TimeUnit refillUnit) {

		final TokenBucket tokenBucket = TokenBuckets.builder()
				.withCapacity(capacity)
				.withFixedIntervalRefillStrategy(refillTokens, refillPeriod, refillUnit)
				.build();

		return (exchange, chain) -> {
			//TODO: get a token bucket for a key
			log.debug("TokenBucket capacity: " + tokenBucket.getCapacity());
			boolean consumed = tokenBucket.tryConsume();
			if (consumed) {
				return chain.filter(exchange);
			}
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			return exchange.getResponse().setComplete();
		};
	}
}
