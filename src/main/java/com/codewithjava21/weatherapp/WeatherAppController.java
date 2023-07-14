package com.codewithjava21.weatherapp;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import jakarta.servlet.http.HttpServletRequest;

@RequestMapping("/weather")
@RestController
public class WeatherAppController {

	private RestTemplate restTemplate;
	
	public WeatherAppController() {
		restTemplate = new RestTemplateBuilder().build();
	}
	
	@GetMapping("/helloworld")
	public ResponseEntity<String> getHello(HttpServletRequest req) {
		return ResponseEntity.ok("Hello world!\n");
	}

	@PutMapping("/latest/{stationid}")
	public ResponseEntity<LatestWeather> putLatestData(HttpServletRequest req,
			@PathVariable(value="stationid") String stationid) {
		
		LatestWeather response = restTemplate.getForObject(
				"https://api.weather.gov/stations/" + stationid + "/observations/latest",
				LatestWeather.class);
		
				
		return ResponseEntity.ok(response);
	}

}
