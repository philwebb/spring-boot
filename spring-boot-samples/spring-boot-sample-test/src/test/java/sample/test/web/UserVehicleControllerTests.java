/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.test.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import sample.test.WelcomeCommandLineRunner;
import sample.test.domain.VehicleIdentificationNumber;
import sample.test.service.VehicleDetails;
import sample.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mvc.MvcTester;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * {@code @WebMvcTest} based tests for {@link UserVehicleController}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@WebMvcTest(UserVehicleController.class)
public class UserVehicleControllerTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber(
			"00000000000000000");

	@Autowired
	private MvcTester mvc;

	@Autowired
	private ApplicationContext applicationContext;

	@MockBean
	private UserVehicleService userVehicleService;

	@Test
	public void getVehicleWhenRequestingTextShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot"))
				.willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get("/sboot/vehicle").accept(MediaType.TEXT_PLAIN)).isOk()
				.contains("Honda Civic");
	}

	@Test
	public void getVehicleWhenRequestingJsonShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot"))
				.willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get("/sboot/vehicle").accept(MediaType.APPLICATION_JSON))
				.isOk().isEqualToJson("{'make':'Honda','model':'Civic'}");
	}

	@Test
	public void getVehicleWhenRequestingHtmlShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot"))
				.willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get("/sboot/vehicle.html").accept(MediaType.TEXT_HTML)).isOk()
				.contains("<h1>Honda Civic</h1>");
	}

	@Test
	public void getVehicleWhenUserNotFoundShouldReturnNotFound() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot"))
				.willThrow(new UserNameNotFoundException("sboot"));
		assertThat(this.mvc.get("/sboot/vehicle")).isNotFound();
	}

	@Test
	public void getVehicleWhenVinNotFoundShouldReturnNotFound() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot"))
				.willThrow(new VehicleIdentificationNumberNotFoundException(VIN));
		assertThat(this.mvc.get("/sboot/vehicle")).isNotFound();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void welcomeCommandLineRunnerShouldBeAvailable() throws Exception {
		// Since we're a @WebMvcTest WelcomeCommandLineRunner should not be available
		assertThat(this.applicationContext.getBean(WelcomeCommandLineRunner.class));
	}

}
