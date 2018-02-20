/*
 * Copyright 2012-2018 the original author or authors.
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
 */

package org.springframework.boot.actuate.health;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.util.CollectionUtils;

/**
 * Reactive {@link EndpointWebExtension} for the {@link HealthEndpoint}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
public class ReactiveHealthEndpointWebExtension {

	private final ReactiveHealthIndicator delegate;

	private final HealthStatusHttpMapper statusHttpMapper;

	private final ShowDetails showDetails;

	private final List<String> roles;

	public ReactiveHealthEndpointWebExtension(ReactiveHealthIndicator delegate,
			HealthStatusHttpMapper statusHttpMapper, ShowDetails showDetails, List<String> roles) {
		this.delegate = delegate;
		this.statusHttpMapper = statusHttpMapper;
		this.showDetails = showDetails;
		this.roles = roles;
	}

	@ReadOperation
	public Mono<WebEndpointResponse<Health>> health(SecurityContext securityContext) {
		return health(securityContext, this.showDetails);
	}

	public Mono<WebEndpointResponse<Health>> health(SecurityContext securityContext,
			ShowDetails showDetails) {
		return this.delegate.health().map((health) -> {
			Integer status = this.statusHttpMapper.mapStatus(health.getStatus());
			if (showDetails == ShowDetails.NEVER
					|| (showDetails == ShowDetails.WHEN_AUTHORIZED
							&& (securityContext.getPrincipal() == null
							|| !isUserInRole(securityContext)))) {
				health = Health.status(health.getStatus()).build();
			}
			return new WebEndpointResponse<>(health, status);
		});
	}

	private boolean isUserInRole(SecurityContext securityContext) {
		if (CollectionUtils.isEmpty(this.roles)) {
			return true;
		}
		for (String role : this.roles) {
			if (securityContext.isUserInRole(role)) {
				return true;
			}
		}
		return false;
	}

}
