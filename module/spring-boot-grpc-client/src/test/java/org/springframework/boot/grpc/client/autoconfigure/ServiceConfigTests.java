/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.ServiceConfigUtil;
import io.grpc.internal.ServiceConfigUtil.LbConfig;
import io.grpc.internal.ServiceConfigUtil.PolicySelection;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServiceConfig}.
 *
 * @author Phillip Webb
 */
class ServiceConfigTests {

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - pickfirst: {}
			""")
	void pickFirstLoadBalancing() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("pick_first");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("PickFirstLoadBalancer");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("shuffleAddressList").isNull();
	}

	@Test
	@WithResource(name = "config.yaml", content = """
			config:
			  load-balancing:
			  - pickfirst:
			      shuffle-address-list: true
			""")
	void pickFirstLoadBalancingWithProperties() throws Exception {
		Map<String, Object> map = bindAndGetAsMap();
		assertThat(map).containsKey("loadBalancingConfig");
		List<Map<String, ?>> loadBalancingConfigs = ServiceConfigUtil.getLoadBalancingConfigsFromServiceConfig(map);
		assertThat(loadBalancingConfigs).hasSize(1);
		assertThat(loadBalancingConfigs.get(0)).containsKey("pick_first");
		PolicySelection loadBalancingPolicySelection = getLoadBalancingPolicySelection(loadBalancingConfigs);
		assertThat(loadBalancingPolicySelection.toString()).contains("PickFirstLoadBalancer");
		assertThat(loadBalancingPolicySelection.getConfig()).extracting("shuffleAddressList").isEqualTo(Boolean.TRUE);
	}

	private PolicySelection getLoadBalancingPolicySelection(List<Map<String, ?>> rawConfigs) {
		List<LbConfig> unwrappedConfigs = ServiceConfigUtil.unwrapLoadBalancingConfigList(rawConfigs);
		LoadBalancerRegistry registry = LoadBalancerRegistry.getDefaultRegistry();
		return (PolicySelection) ServiceConfigUtil.selectLbPolicyFromList(unwrappedConfigs, registry).getConfig();
	}

	private Map<String, Object> bindAndGetAsMap() throws Exception {
		Map<String, Object> map = new LinkedHashMap<>();
		bind().applyTo(map);
		return map;
	}

	private ServiceConfig bind() throws Exception {
		YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
		PropertySource<?> propertySource = loader.load("config.yaml", new ClassPathResource("config.yaml")).get(0);
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addLast(propertySource);
		Binder binder = Binder.get(environment);
		return binder.bind("config", ServiceConfig.class).get();
	}

}
