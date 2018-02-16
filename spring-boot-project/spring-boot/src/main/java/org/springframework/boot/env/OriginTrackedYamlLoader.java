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

package org.springframework.boot.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.boot.yaml.SpringProfileDocumentMatcher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

/**
 * Class to load {@code .yml} files into a map of {@code String} ->
 * {@link OriginTrackedValue}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OriginTrackedYamlLoader extends YamlProcessor {

	private final Resource resource;

	private final Environment environment;

	OriginTrackedYamlLoader(Resource resource, String profile, Environment environment) {
		this.resource = resource;
		if (profile == null) {
			setMatchDefault(true);
			setDocumentMatchers(new OriginTrackedSpringProfileDocumentMatcher());
		}
		else {
			setMatchDefault(false);
			setDocumentMatchers(new OriginTrackedSpringProfileDocumentMatcher(profile));
		}
		setResources(resource);
		this.environment = environment;
	}

	@Override
	protected Yaml createYaml() {
		BaseConstructor constructor = new OriginTrackingConstructor();
		Representer representer = new Representer();
		DumperOptions dumperOptions = new DumperOptions();
		LimitedResolver resolver = new LimitedResolver();
		return new Yaml(constructor, representer, dumperOptions, resolver);
	}

	public List<MapPropertySource> load() {
		List<MapPropertySource> source = new ArrayList<>();
		process((properties, map) -> matchCallback(source, properties, map));
		return source;
	}

	@SuppressWarnings("unchecked")
	private void matchCallback(List<MapPropertySource> source, Properties properties, Map<String, Object> map) {
		String propertySourceName = getName(properties);
		Map<String, Object> flattenedMap = getFlattenedMap(map);
		Set<String> negatedProfiles = (Set<String>) properties
				.get(SpringProfileDocumentMatcher.NEGATED_PROFILES_KEY);
		if (CollectionUtils.isEmpty(negatedProfiles)) {
			source.add(new OriginTrackedMapPropertySource(propertySourceName, flattenedMap));
		}
		else {
			YamlNegationPropertySource propertySource = new YamlNegationPropertySource(
					propertySourceName, flattenedMap,
					OriginTrackedYamlLoader.this.environment,
					negatedProfiles);
			source.add(propertySource);
		}
	}

	private String getName(Properties properties) {
		String documentProfiles = properties.getProperty("spring.profiles",
				"(default)");
		return "YAML [" + documentProfiles + "]";
	}

	/**
	 * {@link Constructor} that tracks property origins.
	 */
	private class OriginTrackingConstructor extends StrictMapAppenderConstructor {

		@Override
		protected Object constructObject(Node node) {
			if (node instanceof ScalarNode) {
				if (!(node instanceof KeyScalarNode)) {
					return constructTrackedObject(node, super.constructObject(node));
				}
			}
			else if (node instanceof MappingNode) {
				replaceMappingNodeKeys((MappingNode) node);
			}
			return super.constructObject(node);
		}

		private void replaceMappingNodeKeys(MappingNode node) {
			node.setValue(node.getValue().stream().map(KeyScalarNode::get)
					.collect(Collectors.toList()));
		}

		private Object constructTrackedObject(Node node, Object value) {
			Origin origin = getOrigin(node);
			return OriginTrackedValue.of(getValue(value), origin);
		}

		private Object getValue(Object value) {
			return (value != null ? value : "");
		}

		private Origin getOrigin(Node node) {
			Mark mark = node.getStartMark();
			Location location = new Location(mark.getLine(), mark.getColumn());
			return new TextResourceOrigin(OriginTrackedYamlLoader.this.resource,
					location);
		}

	}

	/**
	 * {@link ScalarNode} that replaces the key node in a {@link NodeTuple}.
	 */
	private static class KeyScalarNode extends ScalarNode {

		KeyScalarNode(ScalarNode node) {
			super(node.getTag(), node.getValue(), node.getStartMark(), node.getEndMark(),
					node.getStyle());
		}

		public static NodeTuple get(NodeTuple nodeTuple) {
			Node keyNode = nodeTuple.getKeyNode();
			Node valueNode = nodeTuple.getValueNode();
			return new NodeTuple(KeyScalarNode.get(keyNode), valueNode);
		}

		private static Node get(Node node) {
			if (node instanceof ScalarNode) {
				return new KeyScalarNode((ScalarNode) node);
			}
			return node;
		}

	}

	/**
	 * {@link Resolver} that limits {@link Tag#TIMESTAMP} tags.
	 */
	private static class LimitedResolver extends Resolver {

		@Override
		public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
			if (tag == Tag.TIMESTAMP) {
				return;
			}
			super.addImplicitResolver(tag, regexp, first);
		}

	}

	/**
	 * {@link SpringProfileDocumentMatcher} that deals with {@link OriginTrackedValue
	 * OriginTrackedValues}.
	 */
	private static class OriginTrackedSpringProfileDocumentMatcher
			extends SpringProfileDocumentMatcher {

		OriginTrackedSpringProfileDocumentMatcher(String... profiles) {
			super(profiles);
		}

		@Override
		protected List<String> extractSpringProfiles(Properties properties) {
			Properties springProperties = new Properties();
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				if (String.valueOf(entry.getKey()).startsWith("spring.")) {
					Object value = entry.getValue();
					if (value instanceof OriginTrackedValue) {
						value = ((OriginTrackedValue) value).getValue();
					}
					springProperties.put(entry.getKey(), value);
				}
			}
			return super.extractSpringProfiles(springProperties);
		}

	}

}
