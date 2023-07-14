package com.codewithjava21.weatherapp;

import java.time.Instant;

public class Properties {
	
	private String station;
	private Instant timestamp;
	private Measurement temperature;
	private Measurement dewpoint;
	private Measurement windDirection;
	private Measurement windSpeed;
	private Measurement windGust;
	private Measurement barometricPressure;
	private Measurement visibility;
	private Measurement precipitationLastHour;
	private Measurement precipitationlast3Hours;
	private Measurement precipitationlast6Hours;
	private Measurement relativeHumidity;
	private Measurement windChill;
	private Measurement heatIndex;
	private CloudLayer[] cloudLayers;
	
	public String getStation() {
		return station;
	}
	
	public void setStation(String station) {
		this.station = station;
	}
	
	public Instant getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
	
	public Measurement getTemperature() {
		return temperature;
	}
	
	public void setTemperature(Measurement temperature) {
		this.temperature = temperature;
	}
	
	public Measurement getDewpoint() {
		return dewpoint;
	}
	
	public void setDewpoint(Measurement dewpoint) {
		this.dewpoint = dewpoint;
	}
	
	public Measurement getWindDirection() {
		return windDirection;
	}
	
	public void setWindDirection(Measurement windDirection) {
		this.windDirection = windDirection;
	}
	
	public Measurement getWindSpeed() {
		return windSpeed;
	}
	
	public void setWindSpeed(Measurement windSpeed) {
		this.windSpeed = windSpeed;
	}
	
	public Measurement getWindGust() {
		return windGust;
	}
	
	public void setWindGust(Measurement windGust) {
		this.windGust = windGust;
	}
	
	public Measurement getBarometricPressure() {
		return barometricPressure;
	}
	
	public void setBarometricPressure(Measurement barometricPressure) {
		this.barometricPressure = barometricPressure;
	}
	
	public Measurement getVisibility() {
		return visibility;
	}
	
	public void setVisibility(Measurement visibility) {
		this.visibility = visibility;
	}
	
	public Measurement getPrecipitationLastHour() {
		return precipitationLastHour;
	}
	
	public void setPrecipitationLastHour(Measurement precipitationLastHour) {
		this.precipitationLastHour = precipitationLastHour;
	}
	
	public Measurement getPrecipitationlast3Hours() {
		return precipitationlast3Hours;
	}
	
	public void setPrecipitationlast3Hours(Measurement precipitationlast3Hours) {
		this.precipitationlast3Hours = precipitationlast3Hours;
	}
	
	public Measurement getPrecipitationlast6Hours() {
		return precipitationlast6Hours;
	}
	
	public void setPrecipitationlast6Hours(Measurement precipitationlast6Hours) {
		this.precipitationlast6Hours = precipitationlast6Hours;
	}
	
	public Measurement getRelativeHumidity() {
		return relativeHumidity;
	}
	
	public void setRelativeHumidity(Measurement relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}
	
	public Measurement getWindChill() {
		return windChill;
	}
	
	public void setWindChill(Measurement windChill) {
		this.windChill = windChill;
	}
	
	public Measurement getHeatIndex() {
		return heatIndex;
	}
	
	public void setHeatIndex(Measurement heatIndex) {
		this.heatIndex = heatIndex;
	}

	public CloudLayer[] getCloudLayers() {
		return cloudLayers;
	}

	public void setCloudLayers(CloudLayer[] cloudLayers) {
		this.cloudLayers = cloudLayers;
	}
}
