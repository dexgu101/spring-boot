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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.ShowDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 */
@ConfigurationProperties("management.endpoint.health")
public class HealthEndpointProperties {

	/**
	 * Whether to show full health details.
	 */
	private ShowDetails showDetails = ShowDetails.WHEN_AUTHORIZED;

	/**
	 * Roles that a user must have when only showing details to authorized users.
	 */
	private List<String> roles = new ArrayList<>();

	public ShowDetails getShowDetails() {
		return this.showDetails;
	}

	public void setShowDetails(ShowDetails showDetails) {
		this.showDetails = showDetails;
	}

	public List<String> getRoles() {
		return this.roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

}
