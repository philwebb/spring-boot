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
import sample.test.domain.VehicleIdentificationNumber;
import sample.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link UserVehicleController}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@WebMvcTest(UserVehicleController.class)
public class UserVehicleControllerTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber(
			"00000000000000000");

	@Autowired
	private MockMvc mvc;

	@MockBean
	private UserVehicleService userVehicleService;

	@Test
	public void getMakeAndModelShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getMakeAndModel("sboot")).willReturn("Honda Civic");
		this.mvc.perform(get("/sboot/vehicle")).andExpect(status().isOk())
				.andExpect(content().string("Honda Civic"));
	}

	@Test
	public void getMakeAndModelWhenUserNotFoundShouldReturnNotFound() throws Exception {
		given(this.userVehicleService.getMakeAndModel("sboot"))
				.willThrow(new UserNameNotFoundException("sboot"));
		this.mvc.perform(get("/sboot/vehicle")).andExpect(status().isNotFound());
	}

	@Test
	public void getMakeAndModelWhenVinNotFoundShouldReturnNotFound() throws Exception {
		given(this.userVehicleService.getMakeAndModel("sboot"))
				.willThrow(new VehicleIdentificationNumberNotFoundException(VIN));
		this.mvc.perform(get("/sboot/vehicle")).andExpect(status().isNotFound());
	}

}
