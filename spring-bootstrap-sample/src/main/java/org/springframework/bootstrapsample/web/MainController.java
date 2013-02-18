package org.springframework.bootstrapsample.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrapsample.service.CityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainController {

	private CityService cityService;


	@RequestMapping("/")
	@ResponseBody
	public String helloWorld() {
		return "Hello World " + cityService.getCity("Bath", "UK").getName();
	}

	@Autowired
	void setCityService(CityService cityService) {
		this.cityService = cityService;
	}

}
