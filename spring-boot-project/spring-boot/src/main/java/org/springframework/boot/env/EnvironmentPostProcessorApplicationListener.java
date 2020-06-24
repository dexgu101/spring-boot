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

package org.springframework.boot.env;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code spring.factories} file.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	private int order = DEFAULT_ORDER;

	private final MultiValueMap<SpringApplication, LoggingPostProcessor> loggingPostProcessors = new LinkedMultiValueMap<>();

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationPreparedEvent.class.isAssignableFrom(eventType)
				|| ApplicationFailedEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent((ApplicationEnvironmentPreparedEvent) event);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent((ApplicationPreparedEvent) event);
		}
	}

	private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		SpringApplication application = event.getSpringApplication();
		List<EnvironmentPostProcessor> postProcessors = loadPostProcessors(event.getSpringApplication());
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(environment, application);
		}
	}

	private List<EnvironmentPostProcessor> loadPostProcessors(SpringApplication application) {
		List<String> names = SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
				getClass().getClassLoader());
		return loadPostProcessors(application, names);
	}

	private List<EnvironmentPostProcessor> loadPostProcessors(SpringApplication application, List<String> names) {
		List<EnvironmentPostProcessor> postProcessors = new ArrayList<>(names.size());
		for (String name : names) {
			try {
				postProcessors.add(instantiatePostProcessor(application, name));
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Unable to instantiate factory class [" + name
						+ "] for factory type [" + EnvironmentPostProcessor.class.getName() + "]", ex);
			}
		}
		AnnotationAwareOrderComparator.sort(postProcessors);
		return postProcessors;
	}

	private EnvironmentPostProcessor instantiatePostProcessor(SpringApplication application, String name)
			throws Exception {
		Class<?> type = ClassUtils.forName(name, getClass().getClassLoader());
		Assert.isAssignable(EnvironmentPostProcessor.class, type);
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			if (hasSingleLogParameter(constructor)) {
				LoggingPostProcessor loggingPostProcessor = new LoggingPostProcessor(type, constructor);
				this.loggingPostProcessors.add(application, loggingPostProcessor);
				return loggingPostProcessor.getPostProcessor();
			}
		}
		return (EnvironmentPostProcessor) ReflectionUtils.accessibleConstructor(type).newInstance();
	}

	private boolean hasSingleLogParameter(Constructor<?> constructor) {
		return constructor.getParameterCount() == 1 && Log.class.isAssignableFrom(constructor.getParameterTypes()[0]);
	}

	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		List<LoggingPostProcessor> loggingPostProcessors = this.loggingPostProcessors
				.remove(event.getSpringApplication());
		if (loggingPostProcessors != null) {
			loggingPostProcessors.forEach(LoggingPostProcessor::switchLog);
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Manages a {@link EnvironmentPostProcessor} with an injected {@link Log}.
	 */
	private static class LoggingPostProcessor {

		private final DeferredLog log = new DeferredLog();

		private final EnvironmentPostProcessor postProcessor;

		LoggingPostProcessor(Class<?> type, Constructor<?> constructor) throws Exception {
			ReflectionUtils.makeAccessible(constructor);
			this.postProcessor = (EnvironmentPostProcessor) constructor.newInstance(this.log);
		}

		void switchLog() {
			this.log.switchTo(this.postProcessor.getClass());
		}

		EnvironmentPostProcessor getPostProcessor() {
			return this.postProcessor;
		}

	}

}
