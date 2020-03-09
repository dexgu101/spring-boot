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

package org.springframework.boot.maven.layer.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link CustomResourceStrategy}.
 *
 * @author Madhura Bhave
 */
public class CustomResourceStrategyTests {

	private CustomResourceStrategy strategy;

	@Test
	void createWhenFiltersNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CustomResourceStrategy("custom", null));
	}

	@Test
	void createWhenFiltersEmptyShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CustomResourceStrategy("custom", Collections.emptyList()));
	}

	@Test
	void getLayerShouldReturnLayerName() {
		this.strategy = new CustomResourceStrategy("custom", Collections.singletonList(new TestFilter1()));
		assertThat(this.strategy.getLayer().toString()).isEqualTo("custom");
	}

	@Test
	void getMatchinLayerWhenFilterMatchesIncludes() {
		this.strategy = new CustomResourceStrategy("custom", Collections.singletonList(new TestFilter1()));
		assertThat(this.strategy.getMatchingLayer("ABCD").toString()).isEqualTo("custom");
	}

	@Test
	void matchesWhenFilterMatchesIncludesAndExcludesFromSameFilter() {
		this.strategy = new CustomResourceStrategy("custom", Collections.singletonList(new TestFilter1()));
		assertThat(this.strategy.getMatchingLayer("AZ")).isNull();
	}

	@Test
	void matchesWhenFilterMatchesIncludesAndExcludesFromAnotherFilter() {
		List<ResourceFilter> filters = new ArrayList<>();
		filters.add(new TestFilter1());
		filters.add(new TestFilter2());
		this.strategy = new CustomResourceStrategy("custom", filters);
		assertThat(this.strategy.getMatchingLayer("AY")).isNull();
	}

	private static class TestFilter1 implements ResourceFilter {

		@Override
		public boolean isResourceIncluded(String resourceName) {
			return resourceName.startsWith("A");
		}

		@Override
		public boolean isResourceExcluded(String resourceName) {
			return resourceName.endsWith("Z");
		}

	}

	private static class TestFilter2 implements ResourceFilter {

		@Override
		public boolean isResourceIncluded(String resourceName) {
			return resourceName.startsWith("B");
		}

		@Override
		public boolean isResourceExcluded(String resourceName) {
			return resourceName.endsWith("Y");
		}

	}

}
