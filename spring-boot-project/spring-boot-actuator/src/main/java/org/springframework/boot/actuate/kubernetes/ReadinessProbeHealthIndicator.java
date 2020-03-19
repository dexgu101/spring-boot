/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.kubernetes;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.kubernetes.ApplicationStateProvider;
import org.springframework.boot.kubernetes.ReadinessState;

/**
 * A {@link HealthIndicator} that checks the {@link ReadinessState} of the application.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class ReadinessProbeHealthIndicator extends AbstractHealthIndicator {

	private final ApplicationStateProvider applicationStateProvider;

	public ReadinessProbeHealthIndicator(ApplicationStateProvider applicationStateProvider) {
		this.applicationStateProvider = applicationStateProvider;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		if (ReadinessState.ready().equals(this.applicationStateProvider.getReadinessState())) {
			builder.up();
		}
		else {
			builder.outOfService();
		}
	}

}
