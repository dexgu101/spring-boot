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

package org.springframework.boot.convert;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts from a String to a {@link java.lang.Enum} with lenient conversion rules.
 * Specifically:
 * <ul>
 * <li>Uses a case insensitive search</li>
 * <li>Does not consider {@code '_'}, {@code '$'} or other special characters</li>
 * <li>Allows mapping of YAML style {@code "false"} and {@code "true"} to enums {@code ON}
 * and {@code OFF}</li>
 * </ul>
 *
 * @author Phillip Webb
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class LenientStringToEnumConverterFactory extends AbstractTypeToEnumConverterFactory<String> {

	@Override
	public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
		Class<?> enumType = getEnumType(targetType);
		return new StringToEnum(enumType);
	}

	private class StringToEnum<T extends Enum> implements Converter<String, T> {

		private final Class<T> enumType;

		StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(String source) {
			if (source.isEmpty()) {
				return null;
			}
			source = source.trim();
			try {
				return (T) Enum.valueOf(this.enumType, source);
			}
			catch (Exception ex) {
				return findEnum(source);
			}
		}

		private T findEnum(String source) {
			Map<String, T> candidates = new LinkedHashMap<>();
			for (T candidate : (Set<T>) EnumSet.allOf(this.enumType)) {
				candidates.put(getCanonicalName(candidate.name()), candidate);
			}
			String name = getCanonicalName(source);
			T result = candidates.get(name);
			if (result != null) {
				return result;
			}
			throw new IllegalArgumentException("No enum constant " + this.enumType.getCanonicalName() + "." + source);
		}

	}

}
