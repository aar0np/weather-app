package com.codewithjava21.weatherapp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.Route;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;

@Route("")
public class WeatherMainView extends VerticalLayout {

	private static final long serialVersionUID = -4548421578729299243L;

	private Image iconImage = new Image();
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
	
	private record Cloud(int elevation, String desc) {
	}
	
	public WeatherMainView(WeatherAppRepository repo) {
		controller = new WeatherAppController(repo);
		
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
		cloudGrid.setWidth("250px");
		cloudGrid.setHeight("250px");

		add(buildRefresh());
		add(buildStationDataView(), iconImage, dateTime);
		add(buildUnitRadio());
		add(buildTempPrecipView());
		add(buildWindDataView());
		add(visibility, cloudGrid);
	}
	
	private Component buildStationDataView() {
		var layout = new HorizontalLayout();

		layout.add(stationId, month);
		
		return layout;
	}
	
	private Component buildUnitRadio() {
		var layout = new HorizontalLayout();

		unitSelector.setLabel("Units");
		unitSelector.setItems("Celsius/Metric", "Fahrenheit/Imperial");
		unitSelector.setValue("Celsius/Metric");
		
		unitSelector.addValueChangeListener(click -> {
			refreshData();
		});
		
		layout.add(unitSelector);
		
		return layout;		
	}
	
	private Component buildTempPrecipView() {
		var layout = new HorizontalLayout();

		layout.add(temperature, precipitationLastHour);
		
		return layout;
		
	}
	
	private Component buildWindDataView() {
		var layout = new HorizontalLayout();
		
		layout.add(windSpeed, windDirection);
		
		return layout;
	}	
	
	private Component buildRefresh() {
		var layout = new HorizontalLayout();
		var queryButton = new Button("Refresh");
		queryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		
		layout.add(queryButton);
		
		queryButton.addClickListener(click -> {
			refreshData();
		});
		
		return layout;
	}
	
	private void refreshData() {
		ResponseEntity<WeatherEntity> latest = controller.getLatestData(
				stationId.getValue(), Integer.parseInt(month.getValue()));
		WeatherEntity latestWeather = latest.getBody();
		
		Instant time = latestWeather.getPrimaryKey().getTimestamp();
		Float temp = latestWeather.getTemperatureCelsius();
		Float windSpd = latestWeather.getWindSpeedKMH();
		String iconURL = latestWeather.getReadingIcon();
		Integer windDir = latestWeather.getWindDirectionDegrees();
		Integer visib = latestWeather.getVisibilityM();
		Float precip = latestWeather.getPrecipitationLastHour();

		dateTime.setValue(time.toString());
		iconImage.setSrc(iconURL);

		if (!unitSelector.getValue().equals("Celsius/Metric")) {
			temp = computeFahrenheit(temp);
			windSpd = computeMiles(windSpd);
			visib = computeFeet(visib);
		}

		temperature.setValue(temp.toString());
		windSpeed.setValue(windSpd.toString());
		
		if (windSpd > 0) {
			windDirection.setValue(convertWindDirection(windDir));
			windDirection.setVisible(true);
		} else {
			windDirection.setVisible(false);
		}
		
		if (visib > 0) {
			visibility.setValue(visib.toString());
		} else {
			visibility.setValue("Unlimited");
		}
		
		precipitationLastHour.setValue(precip.toString());
		
		List<Cloud> clouds = new ArrayList<>();
		
		for (int key : latestWeather.getCloudCover().keySet()) {

			String description = latestWeather.getCloudCover().get(key);
			if (!unitSelector.getValue().equals("Celsius/Metric")) {
				key = computeFeet(key);
			}
			
			Cloud cloud = new Cloud(key,description);
			clouds.add(cloud);
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
}
