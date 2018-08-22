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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionException;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

/**
 * Adapter class to convert {@link OAuth2ClientProperties} to a
 * {@link ClientRegistration}.
 *
 * @author Phillip Webb
 * @author Thiago Hirata
 * @author Madhura Bhave
 * @since 2.1.0
 */
public final class OAuth2ClientPropertiesRegistrationAdapter {

	private OAuth2ClientPropertiesRegistrationAdapter() {
	}

	public static Map<String, ClientRegistration> getClientRegistrations(
			OAuth2ClientProperties properties) {
		Map<String, ClientRegistration> clientRegistrations = new HashMap<>();
		properties.getRegistration().getLogin()
				.forEach((key, value) -> clientRegistrations.put(key,
						getLoginClientRegistration(key, value,
								properties.getProvider())));
		properties.getRegistration().getAuthorizationCode()
				.forEach((key, value) -> clientRegistrations.put(key,
						getAuthorizationCodeClientRegistration(key, value,
								properties.getProvider())));
		return clientRegistrations;
	}

	private static ClientRegistration getAuthorizationCodeClientRegistration(
			String registrationId,
			OAuth2ClientProperties.AuthorizationCodeClientRegistration properties,
			Map<String, Provider> providers) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Builder builder = getBuilder(map, registrationId, properties, providers);
		map.from(properties::getRedirectUri).to(builder::redirectUriTemplate);
		return builder.build();
	}

	private static Builder getBuilder(PropertyMapper map, String registrationId,
			OAuth2ClientProperties.BaseClientRegistration properties,
			Map<String, Provider> providers) {
		Builder builder = getBuilderFromIssuerIfPossible(registrationId,
				properties.getProvider(), providers);
		if (builder == null) {
			builder = getBuilder(registrationId, properties.getProvider(), providers);
		}
		map.from(properties::getClientId).to(builder::clientId);
		map.from(properties::getClientSecret).to(builder::clientSecret);
		map.from(properties::getClientAuthenticationMethod)
				.as(ClientAuthenticationMethod::new)
				.to(builder::clientAuthenticationMethod);
		map.from(properties::getAuthorizationGrantType).as(AuthorizationGrantType::new)
				.to(builder::authorizationGrantType);
		map.from(properties::getScope).as((scope) -> StringUtils.toStringArray(scope))
				.to(builder::scope);
		map.from(properties::getClientName).to(builder::clientName);
		return builder;
	}

	private static ClientRegistration getLoginClientRegistration(String registrationId,
			OAuth2ClientProperties.LoginClientRegistration properties,
			Map<String, Provider> providers) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Builder builder = getBuilder(map, registrationId, properties, providers);
		map.from(properties::getRedirectUriTemplate).to(builder::redirectUriTemplate);
		return builder.build();
	}

	private static Builder getBuilderFromIssuerIfPossible(String registrationId,
			String configuredProviderId, Map<String, Provider> providers) {
		String providerId = (configuredProviderId != null) ? configuredProviderId
				: registrationId;
		if (providers.containsKey(providerId)) {
			Provider provider = providers.get(providerId);
			String issuer = provider.getIssuerUri();
			if (issuer != null) {
				String cleanedIssuer = cleanIssuerPath(issuer);
				Builder builder = ClientRegistrations
						.fromOidcIssuerLocation(cleanedIssuer)
						.registrationId(registrationId);
				return getBuilder(builder, provider);
			}
		}
		return null;
	}

	private static String cleanIssuerPath(String issuer) {
		if (issuer.endsWith("/")) {
			return issuer.substring(0, issuer.length() - 1);
		}
		return issuer;
	}

	private static Builder getBuilder(String registrationId, String configuredProviderId,
			Map<String, Provider> providers) {
		String providerId = (configuredProviderId != null) ? configuredProviderId
				: registrationId;
		CommonOAuth2Provider provider = getCommonProvider(providerId);
		if (provider == null && !providers.containsKey(providerId)) {
			throw new IllegalStateException(
					getErrorMessage(configuredProviderId, registrationId));
		}
		Builder builder = (provider != null) ? provider.getBuilder(registrationId)
				: ClientRegistration.withRegistrationId(registrationId);
		if (providers.containsKey(providerId)) {
			return getBuilder(builder, providers.get(providerId));
		}
		return builder;
	}

	private static String getErrorMessage(String configuredProviderId,
			String registrationId) {
		return ((configuredProviderId != null)
				? "Unknown provider ID '" + configuredProviderId + "'"
				: "Provider ID must be specified for client registration '"
						+ registrationId + "'");
	}

	private static Builder getBuilder(Builder builder, Provider provider) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(provider::getAuthorizationUri).to(builder::authorizationUri);
		map.from(provider::getTokenUri).to(builder::tokenUri);
		map.from(provider::getUserInfoUri).to(builder::userInfoUri);
		map.from(provider::getJwkSetUri).to(builder::jwkSetUri);
		map.from(provider::getUserNameAttribute).to(builder::userNameAttributeName);
		return builder;
	}

	private static CommonOAuth2Provider getCommonProvider(String providerId) {
		try {
			return ApplicationConversionService.getSharedInstance().convert(providerId,
					CommonOAuth2Provider.class);
		}
		catch (ConversionException ex) {
			return null;
		}
	}

}
