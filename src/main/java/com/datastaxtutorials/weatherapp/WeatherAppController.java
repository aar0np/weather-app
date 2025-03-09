package com.datastaxtutorials.weatherapp;

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

import com.datastax.astra.client.Collection;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.Database;
import com.datastax.astra.client.model.Document;
import com.datastax.astra.client.model.Filter;
import com.datastax.astra.client.model.Filters;
import com.datastax.astra.client.model.FindIterable;
import com.datastax.astra.client.model.FindOptions;
import com.datastax.astra.client.model.Sort;
import com.datastax.astra.client.model.Sorts;
import com.datastaxtutorials.weatherapp.langflow.models.LangflowOutput1;
import com.datastaxtutorials.weatherapp.langflow.models.LangflowResponse;
import com.datastaxtutorials.weatherapp.nws.models.CloudLayer;
import com.datastaxtutorials.weatherapp.nws.models.LatestWeather;
import com.google.gson.Gson;

import java.time.Instant;
import java.time.ZonedDateTime;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.datastax.astra.client.model.Filters.eq;

@RequestMapping("/weather")
@RestController
public class WeatherAppController {

	private Collection<Document> collection;
	private RestTemplate restTemplate;
	//private WeatherAppRepository weatherRepo;
	
	private HttpHeaders langflowHeader;
	
	private static final String LANGFLOW_URL = System.getenv("ASTRA_LANGFLOW_URL");
	private static final String BEARER_TOKEN = System.getenv("ASTRA_DB_APP_TOKEN");
	private static final String API_ENDPOINT = System.getenv("ASTRA_DB_API_ENDPOINT");
	
	//public WeatherAppController(WeatherAppRepository weatherAppRepo) {
	public WeatherAppController() {
		// build Rest template for calling NWS endpoint
		restTemplate = new RestTemplateBuilder().build();

		//// instantiate Cassandra repository
		//weatherRepo = weatherAppRepo;

		// define headers for Langflow API
		langflowHeader = new HttpHeaders();
		langflowHeader.setContentType(MediaType.APPLICATION_JSON);
		langflowHeader.add("Authorization", "Bearer " + BEARER_TOKEN);
		
		// define Astra DB API connection
		DataAPIClient client = new DataAPIClient(BEARER_TOKEN);
		Database dbAPI = client.getDatabase(API_ENDPOINT, "weatherapp");
		collection = dbAPI.getCollection("weather_data");
	}
	
	@GetMapping("/helloworld")
	public ResponseEntity<String> getHello() {
		return ResponseEntity.ok("Hello world!\n");
	}
	
	// Astra DB API calls
	@GetMapping("/astradb/api/latest/station/{stationid}/month/{month}")
	public ResponseEntity<WeatherReading> getLatestAstraAPIData(
			@PathVariable(value="stationid") String stationId,
			@PathVariable(value="month") String monthBucket) {
		
		Filter filters = Filters.and(eq("station_id",(stationId)),(eq("month_bucket",monthBucket)));
		Sort sort = Sorts.descending("timestamp");
		FindOptions findOpts = new FindOptions().sort(sort);
		FindIterable<Document> weatherDocs = collection.find(filters, findOpts);
		List<Document> weatherDocsList = weatherDocs.all();
		
		if (weatherDocsList.size() > 0) {
			Document weatherTopDoc = weatherDocsList.get(0);
			WeatherReading currentReading = mapDocumentToWeatherReading(weatherTopDoc);
			return ResponseEntity.ok(currentReading);
		}
		
		return ResponseEntity.ok(new WeatherReading());
	}
		
	@PutMapping("/astradb/api/latest/station/{stationid}")
	public ResponseEntity<WeatherReading> putLatestAstraAPIData(
			@PathVariable(value="stationid") String stationId) {
		
		LatestWeather response = restTemplate.getForObject(
				"https://api.weather.gov/stations/" + stationId + "/observations/latest",
				LatestWeather.class);
		
		Document weatherDoc = mapLatestWeatherToDocument(response, stationId);
		
		// save weather reading
		collection.insertOne(weatherDoc);

		// build response
		WeatherReading currentReading = mapLatestWeatherToWeatherReading(response);
		
		return ResponseEntity.ok(currentReading);
	}
	
	// Langflow API call
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
		
		return mapLangflowResponseToWeatherReading(outputs);
	}
	
	private WeatherReading mapLangflowResponseToWeatherReading(LangflowOutput1[] outputs) {
		WeatherReading returnVal = new WeatherReading();
		String strMessage = outputs[0].getOutputs()[0].getResults().getMessage().getData().getText();
		strMessage = strMessage.replace("**", "");

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
	
	private Document mapLatestWeatherToDocument(LatestWeather weather, String stationId) {
		
		Document returnVal = new Document();
		
		// use timestamp from response to create date
		Instant timestamp = weather.getProperties().getTimestamp();
		Integer bucket = getBucket(timestamp);
		
		returnVal.put("station_id", stationId);
		returnVal.put("month_bucket", bucket.toString());
		returnVal.put("timestamp", timestamp);
		returnVal.put("reading_icon", weather.getProperties().getIcon());
		returnVal.put("station_coordinates_latitude", weather.getGeometry().getCoordinates()[0]);
		returnVal.put("station_coordinates_longitude", weather.getGeometry().getCoordinates()[1]);
		returnVal.put("temperature_celsius", weather.getProperties().getTemperature().getValue());
		returnVal.put("wind_direction_degrees", (int)weather.getProperties().getWindDirection().getValue());
		returnVal.put("wind_gust_kmh", weather.getProperties().getWindGust().getValue());
		returnVal.put("wind_speed_kmh", weather.getProperties().getWindSpeed().getValue());
		returnVal.put("precipitation_last_hour", weather.getProperties().getPrecipitationLastHour().getValue());
		
		// process cloud layers
		CloudLayer[] clouds = weather.getProperties().getCloudLayers();
		Map<Integer,String> cloudMap = new HashMap<>();
		
		for (CloudLayer layer : clouds) {
			// measurements come back as floats, but we need ints for cloud levels
			cloudMap.put((int)layer.getBase().getValue(), layer.getAmount());
		}
		
		returnVal.put("cloud_cover", cloudMap);
		
		return returnVal;
	}
	
	private WeatherReading mapDocumentToWeatherReading(Document doc) {
		WeatherReading returnVal = new WeatherReading();
		
		returnVal.setStationId(doc.getString("station_id"));
		returnVal.setMonthBucket(doc.getString("month_bucket"));
		returnVal.setStationCoordinatesLatitude(doc.getFloat("station_coordinates_latitude"));
		returnVal.setStationCoordinatesLongitude(doc.getFloat("station_coordinates_longitude"));
		returnVal.setTimestamp(doc.getInstant("timestamp"));
		returnVal.setTemperatureCelsius(doc.getFloat("temperature_celsius"));

		if (doc.getFloat("wind_speed_kmh") != null) {
			returnVal.setWindSpeedKMH(doc.getFloat("wind_speed_kmh"));
		}
		
		if (doc.getInteger("wind_direction_degrees") != null) {
			returnVal.setWindDirectionDegrees(doc.getInteger("wind_direction_degrees"));
		}
		
		if (doc.getFloat("wind_gust_kmh") != null) {
			returnVal.setWindGustKMH(doc.getFloat("wind_gust_kmh"));
		}

		returnVal.setReadingIcon(doc.getString("reading_icon"));
		
		if (doc.getInteger("visibility_m") != null) {
			returnVal.setVisibilityM(doc.getInteger("visibility_m"));
		}
		
		if (doc.getFloat("precipitation_last_hour") != null) {
			returnVal.setPrecipitationLastHour(doc.getFloat("precipitation_last_hour"));
		}
		
		LinkedHashMap<Integer,String> cloudObj = (LinkedHashMap<Integer,String>) doc.get("cloud_cover");	 
		Map<Integer,String> cloudMap = new HashMap<>();
		
		if (cloudObj != null) {
			for (Map.Entry<Integer, String> entry : cloudObj.entrySet()) {

				String description = entry.getValue();
				Object keyObj = entry.getKey();
				Integer key = Integer.parseInt(keyObj.toString());
				
				cloudMap.put(key, description);
			}
		}
		
		returnVal.setCloudCover(cloudMap);
		
		return returnVal;
	}
	
	private WeatherReading mapLatestWeatherToWeatherReading(LatestWeather latest) {
		WeatherReading returnVal = new WeatherReading();
		
		returnVal.setStationId(latest.getProperties().getStation());
		returnVal.setMonthBucket(getBucket(latest.getProperties().getTimestamp()).toString());
		returnVal.setStationCoordinatesLatitude(latest.getGeometry().getCoordinates()[0]);
		returnVal.setStationCoordinatesLongitude(latest.getGeometry().getCoordinates()[1]);
		returnVal.setTimestamp(latest.getProperties().getTimestamp());
		returnVal.setTemperatureCelsius(latest.getProperties().getTemperature().getValue());
		returnVal.setWindSpeedKMH(latest.getProperties().getWindSpeed().getValue());
		returnVal.setWindDirectionDegrees((int)latest.getProperties().getWindDirection().getValue());
		returnVal.setWindGustKMH(latest.getProperties().getWindGust().getValue());
		returnVal.setReadingIcon(latest.getProperties().getIcon());
		returnVal.setVisibilityM((int)latest.getProperties().getVisibility().getValue());
		returnVal.setPrecipitationLastHour(latest.getProperties().getPrecipitationLastHour().getValue());
		
		// process cloud layers
		CloudLayer[] clouds = latest.getProperties().getCloudLayers();
		Map<Integer,String> cloudMap = new HashMap<>();
				
		for (CloudLayer layer : clouds) {
			// measurements come back as floats, but we need ints for cloud levels
			cloudMap.put((int)layer.getBase().getValue(), layer.getAmount());
		}
		
		returnVal.setCloudCover(cloudMap);
		
		return returnVal;
	}
	
	protected Integer getBucket(Instant timestamp) {
		
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
