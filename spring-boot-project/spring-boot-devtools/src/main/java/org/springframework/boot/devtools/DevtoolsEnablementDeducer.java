/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility to deduce if Devtools should be enabled in the current context.
 *
 * @author Madhura Bhave
 */
public final class DevtoolsEnablementDeducer {

	private static final Set<String> SKIPPED_STACK_ELEMENTS;

	private static final Set<String> INCLUDED_STACK_ELEMENTS;

	static {
		Set<String> skipped = new LinkedHashSet<>();
		skipped.add("org.junit.runners.");
		skipped.add("org.junit.platform.");
		skipped.add("org.springframework.boot.test.");
		skipped.add("cucumber.runtime.");
		SKIPPED_STACK_ELEMENTS = Collections.unmodifiableSet(skipped);
	}

	static {
		Set<String> included = new LinkedHashSet<>();
		included.add("org.springframework.boot.devtools.autoconfigure");
		included.add("org.springframework.boot.devtools.env");
		INCLUDED_STACK_ELEMENTS = Collections.unmodifiableSet(included);
	}

	private DevtoolsEnablementDeducer() {
	}

	/**
	 * Checks if a specific {@link StackTraceElement} in the current thread's stacktrace
	 * should cause devtools to be disabled.
	 * @param thread the current thread
	 * @return {@code true} if devtools should be enabled skipped
	 */
	public static boolean shouldEnable(Thread thread) {
		for (StackTraceElement element : thread.getStackTrace()) {
			if (isIncludedStackElement(element)) {
				return true;
			}
			if (isSkippedStackElement(element)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isIncludedStackElement(StackTraceElement element) {
		for (String included : INCLUDED_STACK_ELEMENTS) {
			if (element.getClassName().startsWith(included)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSkippedStackElement(StackTraceElement element) {
		for (String skipped : SKIPPED_STACK_ELEMENTS) {
			if (element.getClassName().startsWith(skipped)) {
				return true;
			}
		}
		return false;
	}

}
