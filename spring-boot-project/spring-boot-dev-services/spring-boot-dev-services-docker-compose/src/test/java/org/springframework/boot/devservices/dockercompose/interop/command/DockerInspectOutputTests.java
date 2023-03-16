/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devservices.dockercompose.interop.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.Config;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.ExposedPort;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.HostConfig;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.NetworkSettings;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.NetworkSettings.PortDto;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerInspectOutput}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DockerInspectOutputTests {

	private final ObjectMapper objectMapper = new ObjectMapper()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	@Test
	void deserialize() throws JsonProcessingException {
		String json = """
				{"Id":"f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc","Created":"2023-02-21T12:35:10.468917704Z","Path":"docker-entrypoint.sh","Args":["redis-server"],"State":{"Status":"running","Running":true,"Paused":false,"Restarting":false,"OOMKilled":false,"Dead":false,"Pid":38657,"ExitCode":0,"Error":"","StartedAt":"2023-02-23T12:55:27.585705588Z","FinishedAt":"2023-02-23T12:46:42.013469854Z"},"Image":"sha256:e79ba23ed43baa22054741136bf45bdb041824f41c5e16c0033ea044ca164b82","ResolvConfPath":"/var/lib/docker/containers/f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc/resolv.conf","HostnamePath":"/var/lib/docker/containers/f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc/hostname","HostsPath":"/var/lib/docker/containers/f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc/hosts","LogPath":"/var/lib/docker/containers/f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc/f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc-json.log","Name":"/redis-docker-redis-1","RestartCount":0,"Driver":"btrfs","Platform":"linux","MountLabel":"","ProcessLabel":"","AppArmorProfile":"","ExecIDs":null,"HostConfig":{"Binds":null,"ContainerIDFile":"","LogConfig":{"Type":"json-file","Config":{}},"NetworkMode":"redis-docker_default","PortBindings":{"6379/tcp":[{"HostIp":"","HostPort":""}]},"RestartPolicy":{"Name":"","MaximumRetryCount":0},"AutoRemove":false,"VolumeDriver":"","VolumesFrom":null,"ConsoleSize":[0,0],"CapAdd":null,"CapDrop":null,"CgroupnsMode":"private","Dns":[],"DnsOptions":[],"DnsSearch":[],"ExtraHosts":[],"GroupAdd":null,"IpcMode":"private","Cgroup":"","Links":null,"OomScoreAdj":0,"PidMode":"","Privileged":false,"PublishAllPorts":false,"ReadonlyRootfs":false,"SecurityOpt":null,"UTSMode":"","UsernsMode":"","ShmSize":67108864,"Runtime":"runc","Isolation":"","CpuShares":0,"Memory":0,"NanoCpus":0,"CgroupParent":"","BlkioWeight":0,"BlkioWeightDevice":null,"BlkioDeviceReadBps":null,"BlkioDeviceWriteBps":null,"BlkioDeviceReadIOps":null,"BlkioDeviceWriteIOps":null,"CpuPeriod":0,"CpuQuota":0,"CpuRealtimePeriod":0,"CpuRealtimeRuntime":0,"CpusetCpus":"","CpusetMems":"","Devices":null,"DeviceCgroupRules":null,"DeviceRequests":null,"MemoryReservation":0,"MemorySwap":0,"MemorySwappiness":null,"OomKillDisable":null,"PidsLimit":null,"Ulimits":null,"CpuCount":0,"CpuPercent":0,"IOMaximumIOps":0,"IOMaximumBandwidth":0,"MaskedPaths":["/proc/asound","/proc/acpi","/proc/kcore","/proc/keys","/proc/latency_stats","/proc/timer_list","/proc/timer_stats","/proc/sched_debug","/proc/scsi","/sys/firmware"],"ReadonlyPaths":["/proc/bus","/proc/fs","/proc/irq","/proc/sys","/proc/sysrq-trigger"]},"GraphDriver":{"Data":null,"Name":"btrfs"},"Mounts":[{"Type":"volume","Name":"9edc7fa2fe6c9e8f67fd31a8649a4b5d7edbc9c1604462e04a5f35d6bfda87c3","Source":"/var/lib/docker/volumes/9edc7fa2fe6c9e8f67fd31a8649a4b5d7edbc9c1604462e04a5f35d6bfda87c3/_data","Destination":"/data","Driver":"local","Mode":"","RW":true,"Propagation":""}],"Config":{"Hostname":"f5af31dae7f6","Domainname":"","User":"","AttachStdin":false,"AttachStdout":true,"AttachStderr":true,"ExposedPorts":{"6379/tcp":{}},"Tty":false,"OpenStdin":false,"StdinOnce":false,"Env":["PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin","GOSU_VERSION=1.16","REDIS_VERSION=7.0.8"],"Cmd":["redis-server"],"Image":"redis:7.0","Volumes":{"/data":{}},"WorkingDir":"/data","Entrypoint":["docker-entrypoint.sh"],"OnBuild":null,"Labels":{"com.docker.compose.config-hash":"cfdc8e119d85a53c7d47edb37a3b160a8c83ba48b0428ebc07713befec991dd0","com.docker.compose.container-number":"1","com.docker.compose.depends_on":"","com.docker.compose.image":"sha256:e79ba23ed43baa22054741136bf45bdb041824f41c5e16c0033ea044ca164b82","com.docker.compose.oneoff":"False","com.docker.compose.project":"redis-docker","com.docker.compose.project.config_files":"compose.yaml","com.docker.compose.project.working_dir":"/","com.docker.compose.service":"redis","com.docker.compose.version":"2.16.0"}},"NetworkSettings":{"Bridge":"","SandboxID":"3df878d8ed31b2686e41437f141bebba8afcf3bdf8c47ea07c34c2e0b365ec88","HairpinMode":false,"LinkLocalIPv6Address":"","LinkLocalIPv6PrefixLen":0,"Ports":{"6379/tcp":[{"HostIp":"0.0.0.0","HostPort":"32770"},{"HostIp":"::","HostPort":"32770"}]},"SandboxKey":"/var/run/docker/netns/3df878d8ed31","SecondaryIPAddresses":null,"SecondaryIPv6Addresses":null,"EndpointID":"","Gateway":"","GlobalIPv6Address":"","GlobalIPv6PrefixLen":0,"IPAddress":"","IPPrefixLen":0,"IPv6Gateway":"","MacAddress":"","Networks":{"redis-docker_default":{"IPAMConfig":null,"Links":null,"Aliases":["redis-docker-redis-1","redis","f5af31dae7f6"],"NetworkID":"9cb2b8b6fb20703841b9337b48e65ed2a71e2da2e995e4782066d146c44fc205","EndpointID":"e155c61c1608b20ba7a0bd34790fc342ec576310f75ef4399e96bf3a67e8b3f6","Gateway":"192.168.32.1","IPAddress":"192.168.32.2","IPPrefixLen":20,"IPv6Gateway":"","GlobalIPv6Address":"","GlobalIPv6PrefixLen":0,"MacAddress":"02:42:c0:a8:20:02","DriverOpts":null}}}}
				""";
		List<DockerInspectOutput> deserialized = DockerInspectOutput.parse(this.objectMapper, json);
		Assertions.assertThat(deserialized)
			.containsExactly(new DockerInspectOutput("f5af31dae7f665bd194ec7261bdc84e5df9c64753abb4a6cec6c33f7cf64c3fc",
					new Config("redis:7.0",
							linkedMapOf("com.docker.compose.config-hash",
									"cfdc8e119d85a53c7d47edb37a3b160a8c83ba48b0428ebc07713befec991dd0",
									"com.docker.compose.container-number", "1", "com.docker.compose.depends_on", "",
									"com.docker.compose.image",
									"sha256:e79ba23ed43baa22054741136bf45bdb041824f41c5e16c0033ea044ca164b82",
									"com.docker.compose.oneoff", "False", "com.docker.compose.project", "redis-docker",
									"com.docker.compose.project.config_files", "compose.yaml",
									"com.docker.compose.project.working_dir", "/", "com.docker.compose.service",
									"redis", "com.docker.compose.version", "2.16.0"),
							Map.of("6379/tcp", new ExposedPort()),
							List.of("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
									"GOSU_VERSION=1.16", "REDIS_VERSION=7.0.8")),
					new NetworkSettings(
							Map.of("6379/tcp", List.of(new PortDto("0.0.0.0", "32770"), new PortDto("::", "32770")))),
					new HostConfig("redis-docker_default")));
	}

	@SuppressWarnings("unchecked")
	private <K, V> LinkedHashMap<K, V> linkedMapOf(Object... values) {
		LinkedHashMap<K, V> result = new LinkedHashMap<>();
		for (int i = 0; i < values.length; i = i + 2) {
			result.put((K) values[i], (V) values[i + 1]);
		}
		return result;
	}

	@Test
	void isIpV4() {
		PortDto port = new PortDto("0.0.0.0", "80");
		assertThat(port.isIpV4()).isTrue();
	}

	@Test
	void isNotIpV4becauseIPv6() {
		PortDto port = new PortDto("::1", "80");
		assertThat(port.isIpV4()).isFalse();
	}

	@Test
	void isIpV4becauseEmpty() {
		PortDto port = new PortDto("", "80");
		assertThat(port.isIpV4()).isTrue();
	}

	@Test
	void hostPortAsInt() {
		PortDto port = new PortDto("::1", "80");
		assertThat(port.hostPortAsInt()).isEqualTo(80);
	}

	@Test
	void envAsMap() {
		Config config = new Config("image", Collections.emptyMap(), Collections.emptyMap(),
				List.of("VAR1=a", "VAR2=", "VAR3"));
		Map<String, String> expected = new HashMap<>();
		expected.put("VAR1", "a");
		expected.put("VAR2", "");
		expected.put("VAR3", null);
		assertThat(config.envAsMap()).containsExactlyInAnyOrderEntriesOf(expected);
	}

	@Test
	void isHostNetwork() {
		HostConfig hostConfig = new HostConfig("host");
		assertThat(hostConfig.isHostNetwork()).isTrue();
	}

	@Test
	void isNotHostNetwork() {
		HostConfig hostConfig = new HostConfig("redis-docker_default");
		assertThat(hostConfig.isHostNetwork()).isFalse();
	}

}
