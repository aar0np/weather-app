package com.codewithjava21.weatherapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/weather")
@RestController
public class WeatherAppController {

	public WeatherAppController() {
		
	}
	
	@GetMapping("/helloworld")
	public ResponseEntity<String> getHello() {
		return ResponseEntity.ok("Hello world!\n");
	}
	
}
