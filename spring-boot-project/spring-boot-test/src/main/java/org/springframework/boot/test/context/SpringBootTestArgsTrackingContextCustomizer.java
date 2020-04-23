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

package org.springframework.boot.test.context;

import java.util.Arrays;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to track application arguments that are used in a
 * {@link SpringBootTest}. The application arguments are taken into account when
 * evaluating a {@link MergedContextConfiguration} to determine if a context can be shared
 * between tests.
 *
 * @author Madhura Bhave
 */
class SpringBootTestArgsTrackingContextCustomizer implements ContextCustomizer {

	static final String[] NO_ARGS = new String[0];

	private String[] args;

	SpringBootTestArgsTrackingContextCustomizer(Class<?> testClass) {
		this.args = MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.get(SpringBootTest.class).getValue("args", String[].class).orElse(NO_ARGS);
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {

	}

	/**
	 * Return the application arguments being tracked by this customizer.
	 * @return the args
	 */
	String[] getArgs() {
		return this.args;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass())
				&& Arrays.equals(this.args, ((SpringBootTestArgsTrackingContextCustomizer) obj).args);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.args);
	}

}
