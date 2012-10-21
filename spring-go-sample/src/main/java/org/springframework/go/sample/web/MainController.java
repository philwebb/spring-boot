package org.springframework.go.sample.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MainController {

	@RequestMapping({"/", "index.html"})
	@ResponseBody
	public String welcome() {
		return "Welcome";
	}

}
