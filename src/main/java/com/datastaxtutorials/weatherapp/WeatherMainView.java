package com.datastaxtutorials.weatherapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.Route;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.http.ResponseEntity;

@Route("")
public class WeatherMainView extends VerticalLayout {

	private static final long serialVersionUID = -4548421578729299243L;

	private Image iconImage = new Image();
	private Paragraph tempLarge = new Paragraph();
	private TextField stationId = new TextField("Station ID");
	private TextField month = new TextField("Year/Month");
	private TextField dateTime = new TextField("Date/Time");
	private TextField temperature = new TextField("Temperature");
	private TextField windSpeed = new TextField("Wind Speed");
	private TextField windDirection = new TextField("Wind Direction");
	private TextField visibility = new TextField("Visibility");
	private TextField precipitationLastHour = new TextField("Precipitation 1 hour");
	private RadioButtonGroup<String> unitSelector = new RadioButtonGroup<>();
	private Grid<Cloud> cloudGrid = new Grid<>(Cloud.class);
	
	private WeatherAppController controller;
	private WeatherReading latestWeather = new WeatherReading();
	
	public record Cloud(int elevation, String desc) {
	}
	
	//public WeatherMainView(WeatherAppRepository repo) {
	public WeatherMainView() {
		//controller = new WeatherAppController(repo);
		controller = new WeatherAppController();
		
		// set default values
		Integer monthBucket = controller.getBucket(Instant.now());
		month.setValue(monthBucket.toString());
		stationId.setValue("kmsp");
		
		// configure grid
		cloudGrid.addColumn(Cloud::elevation)
			.setWidth("100px")
			.setHeader("Elevation");
		cloudGrid.addColumn(Cloud::desc)
			.setWidth("150px")
			.setHeader("Description");
		cloudGrid.setWidth("200px");
		cloudGrid.setHeight("200px");

		// set icon size
		iconImage.setWidth("500px");
		
		// compose layout
		add(buildControls());
		HorizontalLayout horizontalLayout = new HorizontalLayout(); 
		VerticalLayout verticalLayout = new VerticalLayout();
		
		horizontalLayout.add(buildIconView());
		
		verticalLayout.add(buildUnitRadio());
		verticalLayout.add(buildStationDataView());
		verticalLayout.add(buildTempPrecipView());
		verticalLayout.add(buildCloudVisibilityView());
		
		horizontalLayout.add(verticalLayout);
		
		add(horizontalLayout);
	}
	
	private Component buildIconView() {
		HorizontalLayout layout = new HorizontalLayout();
		
		// set icon size
		iconImage.setWidth("500px");
		
		// configure temperature display
		tempLarge.getStyle().set("font-size", "150px");
		tempLarge.getStyle().set("font-weight", "bold");
		tempLarge.getStyle().set("color", "#FFFFFF");
		tempLarge.getStyle().set("text-shadow", "1px 1px 2px black");
		tempLarge.getStyle().set("position", "absolute");
		tempLarge.getStyle().set("left", "40px");
		tempLarge.getStyle().set("top", "140px");
		
		layout.add(iconImage, tempLarge);
		
		return layout;
	}
	
	private Component buildControls() {
		HorizontalLayout layout = new HorizontalLayout();
		Button langflowButton = new Button("Langflow Refresh");
		langflowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Button astraButton = new Button("Astra DB Refresh");
		astraButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		
		layout.add(langflowButton, astraButton);
		
		langflowButton.addClickListener(click -> {
			refreshLangflow();
		});
		
		astraButton.addClickListener(click -> {
			refreshAstra();
		});
		
		return layout;
	}
	
	private Component buildUnitRadio() {
		HorizontalLayout layout = new HorizontalLayout();

		unitSelector.setLabel("Units");
		unitSelector.setItems("Celsius/Metric", "Fahrenheit/Imperial");
		unitSelector.setValue("Celsius/Metric");
		
		unitSelector.addValueChangeListener(click -> {
			refreshData(latestWeather);
		});
		
		layout.add(unitSelector);
		
		return layout;		
	}

	private Component buildStationDataView() {
		HorizontalLayout layout = new HorizontalLayout();

		//layout.add(stationId, month, iconImage, dateTime);
		layout.add(stationId, month, dateTime);
		
		return layout;
	}
	
	private Component buildTempPrecipView() {
		HorizontalLayout layout = new HorizontalLayout();

		layout.add(temperature, precipitationLastHour, windSpeed, windDirection);
		
		return layout;
	}
	
	private Component buildCloudVisibilityView() {
		HorizontalLayout layout = new HorizontalLayout();
		
		layout.add(cloudGrid, visibility);
		return layout;
	}
	
	private void refreshAstra() {
		//ResponseEntity<WeatherReading> latest = controller.getLatestData(
		//		stationId.getValue(), Integer.parseInt(month.getValue()));
		
		ResponseEntity<WeatherReading> latest = controller.getLatestAstraAPIData(
				stationId.getValue(), Integer.parseInt(month.getValue()));
		latestWeather = latest.getBody();

		refreshData(latestWeather);
	}
	
	private void refreshLangflow() {

		String message = "Please retrieve the latest weather data (including the weather icon url) in a text format using this endpoint: https://api.weather.gov/stations/KMSP/observations/latest";
		latestWeather = controller.askAgent(new AgentRequest(message));
	
		refreshData(latestWeather);
	}
	
	private void refreshData(WeatherReading latestWeather) {
		
		Instant time = latestWeather.getTimestamp();
		Float temp = latestWeather.getTemperatureCelsius();
		Float windSpd = latestWeather.getWindSpeedKMH();
		String iconURL = latestWeather.getReadingIcon();
		Integer windDir = latestWeather.getWindDirectionDegrees();
		Integer visib = latestWeather.getVisibilityM();
		Float precip = latestWeather.getPrecipitationLastHour();

		StringBuilder tempBuilder = new StringBuilder();
				
		if (!unitSelector.getValue().equals("Celsius/Metric")) {
			DecimalFormat df = new DecimalFormat("0");
			float localTemp = computeFahrenheit(temp);
			
			if (localTemp > -1f) {
				tempLarge.getStyle().set("left", "90px");
			} else {
				tempLarge.getStyle().set("left", "40px");
			}

			tempBuilder.append(df.format(localTemp));
			tempBuilder.append("°F");
			windSpd = computeMiles(windSpd);
			visib = computeFeet(visib);
			precip = computeInches(precip);
		} else {
			if (temp > -1f) {
				tempLarge.getStyle().set("left", "90px");
			} else {
				tempLarge.getStyle().set("left", "40px");
			}

			tempBuilder.append(temp);
			tempBuilder.append("°C");
		}
		
		tempLarge.setText(tempBuilder.toString());
		temperature.setValue(tempBuilder.toString());
		windSpeed.setValue(windSpd.toString());
		precipitationLastHour.setValue(precip.toString());
		dateTime.setValue(time.toString());
		
		if (iconURL != null && iconURL.contains("http")) {
			iconImage.setSrc(iconURL);
		} else {
			iconImage.setSrc("https://api.weather.gov/icons/land/day/skc?size=medium");
		}

		if (visib > 0) {
			visibility.setValue(visib.toString());
		} else {
			visibility.setValue("Unlimited");
		}

		if (windSpd > 0) {
			windDirection.setValue(convertWindDirection(windDir));
			windDirection.setVisible(true);
		} else {
			windDirection.setVisible(false);
		}
						
		List<Cloud> clouds = new ArrayList<>();
		
		if (latestWeather.getCloudCover() != null) {
			for (Entry<Integer, String> entry : latestWeather.getCloudCover().entrySet()) {
	
				String description = entry.getValue();
				Integer key = entry.getKey();

				if (!unitSelector.getValue().equals("Celsius/Metric")) {
					key = computeFeet(key);
				}
				
				Cloud cloud = new Cloud(key,description);
				clouds.add(cloud);
			}
		}
		
		cloudGrid.setItems(clouds);
	}
	
	private String convertWindDirection(Integer degrees) {
		StringBuilder returnVal = new StringBuilder();
		
		if ((degrees > 338 && degrees <= 360) ||
				degrees >= 0 && degrees < 23) {
			returnVal.append("North");
		} else if (degrees > 22 && degrees < 68) {
			returnVal.append("Northeast");
		} else if (degrees > 67 && degrees < 113) {
			returnVal.append("East");
		} else if (degrees > 112 && degrees < 158) {
			returnVal.append("Southeast");
		} else if (degrees > 157 && degrees < 203) {
			returnVal.append("South");
		} else if (degrees > 202 && degrees < 248) {
			returnVal.append("Southwest");
		} else if (degrees > 247 && degrees < 293) {
			returnVal.append("West");
		} else {
			returnVal.append("Northwest");
		}

		return returnVal.toString();
	}
	
	private float computeFahrenheit(float celsius) {
		return (celsius * 9 / 5) + 32;
	}
	
	private float computeMiles(float kilometers) {
		return (kilometers * 1.609F);
	}
	
	private int computeFeet(int meters) {
		return (int)(meters * 3.281F);
	}
	
	private float computeInches(float millimeters) {
		return millimeters / 25.4F;
	}
}
