package com.codewithjava21.weatherapp;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.codewithjava21.weatherapp.langflow.models.LangflowOutput1;
import com.codewithjava21.weatherapp.langflow.models.LangflowResponse;
import com.codewithjava21.weatherapp.nws.models.CloudLayer;
import com.codewithjava21.weatherapp.nws.models.LatestWeather;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZonedDateTime;

import java.util.Map;
import java.util.HashMap;

@RequestMapping("/weather")
@RestController
public class WeatherAppController {

	private RestTemplate restTemplate;
	private WeatherAppRepository weatherRepo;

	private HttpHeaders langflowHeader;
	
	private static final String LANGFLOW_URL = System.getenv("ASTRA_LANGFLOW_URL");
	private static final String BEARER_TOKEN = System.getenv("ASTRA_DB_APP_TOKEN");
	
	public WeatherAppController(WeatherAppRepository weatherAppRepo) {
		// build Rest template for calling NWS and Langflow endpoints
		restTemplate = new RestTemplateBuilder().build();

		// instantiate Cassandra repository
		weatherRepo = weatherAppRepo;

		langflowHeader = new HttpHeaders();
		langflowHeader.setContentType(MediaType.APPLICATION_JSON);
		langflowHeader.add("Authorization", "Bearer " + BEARER_TOKEN);
	}
	
	@GetMapping("/helloworld")
	public ResponseEntity<String> getHello() {
		return ResponseEntity.ok("Hello world!\n");
	}

	@PutMapping("/latest/station/{stationid}")
	public ResponseEntity<WeatherReading> putLatestData(
			@PathVariable(value="stationid") String stationId) {
		
		LatestWeather response = restTemplate.getForObject(
				"https://api.weather.gov/stations/" + stationId + "/observations/latest",
				LatestWeather.class);
		
		// map latest reading to a WeatherEntity
		WeatherEntity weatherEntity = mapLatestWeatherToWeatherEntity(response, stationId);
		
		// save weather reading
		weatherRepo.save(weatherEntity);
		
		WeatherReading currentReading = mapWeatherEntityToWeatherReading(weatherEntity);
		
		return ResponseEntity.ok(currentReading);
	}
	
	@GetMapping("/latest/station/{stationid}/month/{month}")
	public ResponseEntity<WeatherReading> getLatestData(
			@PathVariable(value="stationid") String stationId,
			@PathVariable(value="month") int monthBucket) {
		
		WeatherEntity recentWeather =
				weatherRepo.findByStationIdAndMonthBucket(stationId, monthBucket);
		
		WeatherReading currentReading = mapWeatherEntityToWeatherReading(recentWeather);
		
		if (currentReading != null) {
			return ResponseEntity.ok(currentReading);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	public WeatherReading askAgent (AgentRequest req) {
		
		String reqJSON = new Gson().toJson(req);
		HttpEntity<String> requestEntity = new HttpEntity<>(reqJSON, langflowHeader);

		ResponseEntity<LangflowResponse> resp = restTemplate.exchange(LANGFLOW_URL,
				HttpMethod.POST,
				requestEntity,
				LangflowResponse.class
				);
		
		LangflowResponse lfResp = resp.getBody();
		LangflowOutput1[] outputs = lfResp.getOutputs();
		// String strMessage = outputs[0].getOutputs()[0].getResults().getMessage().getData().getText();
		
		return mapLangflowResponseToWeatherReading(outputs);
	}
	
	private WeatherReading mapLangflowResponseToWeatherReading(LangflowOutput1[] outputs) {
		WeatherReading returnVal = new WeatherReading();
		String strMessage = outputs[0].getOutputs()[0].getResults().getMessage().getData().getText();
		strMessage = strMessage.replace("**", "");
		
		//JsonObject gson = new Gson().fromJson(strMessage, JsonObject.class);
		//JsonObject properties = gson.get("properties").getAsJsonObject();
		//JsonObject temperature = properties.get("temperature").getAsJsonObject();	
		
		//System.out.println("Message: " + strMessage);

		int stationPos = strMessage.indexOf("Raw Message:");
		int tempPos = strMessage.indexOf("Temperature:");
		int degreeTempPos = strMessage.indexOf("°", tempPos + 13);
		int timestampPos = strMessage.indexOf("Timestamp:");
		int iconPos = strMessage.indexOf("[Weather Icon]");
		int iconRightParen = strMessage.indexOf(")", iconPos + 16);
		int windSpeedPos = strMessage.indexOf("Wind Speed:");
		int windKmPos = strMessage.indexOf("km/h", windSpeedPos + 12);
		int windDirPos = strMessage.indexOf("Wind Direction:");
		int windDirDegPos = strMessage.indexOf("°", windDirPos + 16);
		int visPos = strMessage.indexOf("Visibility:");
		int visMPos = strMessage.indexOf("m", visPos + 12);
		int cloudPos = strMessage.indexOf("Cloud Layers:");
		int nextColon = strMessage.indexOf(":", cloudPos + 14);
		
		returnVal.setStationId(strMessage.substring(stationPos + 13, stationPos + 17).toLowerCase());
		returnVal.setTemperatureCelsius(Float.parseFloat(strMessage.substring(tempPos + 13, degreeTempPos)));
		returnVal.setTimestamp(Instant.parse(strMessage.substring(timestampPos + 11, timestampPos + 36)));		
		returnVal.setReadingIcon(strMessage.substring(iconPos + 15, iconRightParen));
		returnVal.setWindSpeedKMH(Float.parseFloat(strMessage.substring(windSpeedPos + 12, windKmPos)));
		returnVal.setWindDirectionDegrees(Integer.parseInt(strMessage.substring(windDirPos + 16, windDirDegPos)));
		returnVal.setVisibilityM(Integer.parseInt(strMessage.substring(visPos + 12, visMPos).replace(",", "").trim()));

		StringBuilder cloudLayers = new StringBuilder();
		
		if (nextColon > 0) {
			cloudLayers.append(strMessage.substring(cloudPos + 14, nextColon));	
		} else {
			cloudLayers.append(strMessage.substring(cloudPos + 14));				
		}
		
		String cloudLayer = cloudLayers.toString();
		
		Map<Integer,String> cloudMap = new HashMap<>();
		int cloudsAt = cloudLayer.indexOf("clouds at");
		int start = 0;
		
		while (cloudsAt > 0) {
			String strClouds = cloudLayer.substring(start, cloudsAt).toLowerCase();
			
			if (strClouds.contains("few")) {
				strClouds = "FEW";
			} else if (strClouds.contains("scattered") || strClouds.contains("sct")) {
				strClouds = "SCT";
			} else if (strClouds.contains("broken") || strClouds.contains("bkn")) {
				strClouds = "BKN";
			} else if (strClouds.contains("overcast") || strClouds.contains("ovc")) {
				strClouds = "OVC";
			} else {
				strClouds = "CLR";
			}
			
			int cloudMPos = cloudLayer.indexOf("m", cloudsAt + 10);
			Float cloudLevel = Float.parseFloat(cloudLayer.substring(cloudsAt + 10, cloudMPos).replace(",", "").trim());

			cloudMap.put(cloudLevel.intValue(), strClouds);
			
			cloudsAt = cloudLayer.indexOf("clouds at", cloudsAt + 1);
			start = cloudsAt - 10;
		}
		
		returnVal.setCloudCover(cloudMap);
		
		return returnVal;
	}
	
	private WeatherEntity mapLatestWeatherToWeatherEntity(LatestWeather weather, String stationId) {
		
		WeatherEntity returnVal = new WeatherEntity();
		
		// use timestamp from response to create date
		Instant timestamp = weather.getProperties().getTimestamp();
		int bucket = getBucket(timestamp);
		
		// gen PK
		WeatherPrimaryKey key = new WeatherPrimaryKey(stationId, bucket, timestamp);
		
		returnVal.setPrimaryKey(key);
		returnVal.setReadingIcon(weather.getProperties().getIcon());
		returnVal.setStationCoordinatesLatitude(weather.getGeometry().getCoordinates()[0]);
		returnVal.setStationCoordinatesLongitude(weather.getGeometry().getCoordinates()[1]);
		returnVal.setTemperatureCelsius(weather.getProperties().getTemperature().getValue());
		returnVal.setWindDirectionDegrees((int)weather.getProperties().getWindDirection().getValue());
		returnVal.setWindGustKMH(weather.getProperties().getWindGust().getValue());
		returnVal.setPrecipitationLastHour(weather.getProperties().getPrecipitationLastHour().getValue());
		
		// process cloud layers
		CloudLayer[] clouds = weather.getProperties().getCloudLayers();
		Map<Integer,String> cloudMap = new HashMap<>();
		
		for (CloudLayer layer : clouds) {
			// measurements come back as floats, but we need ints for cloud levels
			cloudMap.put((int)layer.getBase().getValue(), layer.getAmount());
		}
		
		returnVal.setCloudCover(cloudMap);
		
		return returnVal;
	}
	
	private WeatherReading mapWeatherEntityToWeatherReading(WeatherEntity entity) {
		WeatherReading returnVal = new WeatherReading();
		
		returnVal.setStationId(entity.getPrimaryKey().getStationId());
		returnVal.setMonthBucket(entity.getPrimaryKey().getMonthBucket());
		returnVal.setStationCoordinatesLatitude(entity.getStationCoordinatesLatitude());
		returnVal.setStationCoordinatesLongitude(entity.getStationCoordinatesLongitude());
		returnVal.setTimestamp(entity.getPrimaryKey().getTimestamp());
		returnVal.setTemperatureCelsius(entity.getTemperatureCelsius());
		returnVal.setWindSpeedKMH(entity.getWindSpeedKMH());
		returnVal.setWindDirectionDegrees(entity.getWindDirectionDegrees());
		returnVal.setWindGustKMH(entity.getWindGustKMH());
		returnVal.setReadingIcon(entity.getReadingIcon());
		returnVal.setVisibilityM(entity.getVisibilityM());
		returnVal.setPrecipitationLastHour(entity.getPrecipitationLastHour());
		returnVal.setCloudCover(entity.getCloudCover());
		
		return returnVal;
	}
	
	protected int getBucket(Instant timestamp) {
		
		ZonedDateTime date = ZonedDateTime.parse(timestamp.toString());
		// parse date into year and month to create the month bucket
		Integer year = date.getYear();
		Integer month = date.getMonthValue();
		StringBuilder bucket = new StringBuilder(year.toString());
		
		if (month < 10) {
			bucket.append("0");
		}
		bucket.append(month);

		return Integer.parseInt(bucket.toString());
	}
}
