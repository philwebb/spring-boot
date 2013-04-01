package org.test

@Grab("org.springframework.bootstrap:spring-bootstrap-web-application:0.0.1-SNAPSHOT")

@org.springframework.bootstrap.context.annotation.EnableAutoConfiguration
@org.springframework.stereotype.Controller
class Example {

	@org.springframework.beans.factory.annotation.Autowired
	private MyService myService;

	@org.springframework.web.bind.annotation.RequestMapping("/")
	@org.springframework.web.bind.annotation.ResponseBody
	public String helloWorld() {
		return myService.sayWorld();
	}

}


@org.springframework.stereotype.Service
class MyService {

	public String sayWorld() {
		return "World!";
	}
}


