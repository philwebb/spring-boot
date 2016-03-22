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

import sample.test.domain.User;
import sample.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to return vehicle information for a given {@link User}
 *
 * @author Phillip Webb
 */
@RestController
public class UserVehicleController {

	private UserVehicleService userVehicleService;

	public UserVehicleController(UserVehicleService userVehicleService) {
		this.userVehicleService = userVehicleService;
	}

	@RequestMapping(path = "/{username}/vehicle", method = RequestMethod.GET)
	public String getMakeAndModel(@PathVariable String username) {
		return this.userVehicleService.getMakeAndModel(username);
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	private void handleVinNotFound(VehicleIdentificationNumberNotFoundException ex) {
	}

}
