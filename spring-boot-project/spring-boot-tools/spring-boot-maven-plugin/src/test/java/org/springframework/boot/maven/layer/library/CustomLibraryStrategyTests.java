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

package org.springframework.boot.maven.layer.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 *
 * Tests for {@link CustomLibraryStrategy}.
 *
 * @author Madhura Bhave
 */
public class CustomLibraryStrategyTests {

	private CustomLibraryStrategy strategy;

	@Test
	void createWhenFiltersNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CustomLibraryStrategy("custom", null));
	}

	@Test
	void createWhenFiltersEmptyShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CustomLibraryStrategy("custom", Collections.emptyList()));
	}

	@Test
	void getLayerShouldReturnLayerName() {
		this.strategy = new CustomLibraryStrategy("custom", Collections.singletonList(new TestFilter1Library()));
		assertThat(this.strategy.getLayer().toString()).isEqualTo("custom");
	}

	@Test
	void getMatchinLayerWhenFilterMatchesIncludes() {
		this.strategy = new CustomLibraryStrategy("custom", Collections.singletonList(new TestFilter1Library()));
		Library library = mockLibrary("A-Compile", LibraryScope.COMPILE);
		assertThat(this.strategy.getMatchingLayer(library).toString()).isEqualTo("custom");
	}

	@Test
	void matchesWhenFilterMatchesIncludesAndExcludesFromSameFilter() {
		this.strategy = new CustomLibraryStrategy("custom", Collections.singletonList(new TestFilter1Library()));
		Library library = mockLibrary("A-Runtime", LibraryScope.RUNTIME);
		assertThat(this.strategy.getMatchingLayer(library)).isNull();
	}

	@Test
	void matchesWhenFilterMatchesIncludesAndExcludesFromAnotherFilter() {
		List<LibraryFilter> filters = new ArrayList<>();
		filters.add(new TestFilter1Library());
		filters.add(new TestFilter2Library());
		this.strategy = new CustomLibraryStrategy("custom", filters);
		Library library = mockLibrary("A-Provided", LibraryScope.PROVIDED);
		assertThat(this.strategy.getMatchingLayer(library)).isNull();
	}

	private Library mockLibrary(String name, LibraryScope runtime) {
		Library library = mock(Library.class);
		given(library.getName()).willReturn(name);
		given(library.getScope()).willReturn(runtime);
		return library;
	}

	private static class TestFilter1Library implements LibraryFilter {

		@Override
		public boolean isLibraryIncluded(Library library) {
			return library.getName().contains("A");
		}

		@Override
		public boolean isLibraryExcluded(Library library) {
			return library.getScope().equals(LibraryScope.RUNTIME);
		}

	}

	private static class TestFilter2Library implements LibraryFilter {

		@Override
		public boolean isLibraryIncluded(Library library) {
			return library.getName().contains("B");
		}

		@Override
		public boolean isLibraryExcluded(Library library) {
			return library.getScope().equals(LibraryScope.PROVIDED);
		}

	}

}
