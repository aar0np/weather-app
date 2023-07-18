package com.codewithjava21.weatherapp;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class WeatherMainView extends VerticalLayout {

	private static final long serialVersionUID = -4548421578729299243L;

	private WeatherAppController controller;

	public WeatherMainView(WeatherAppRepository repo) {
		controller = new WeatherAppController(repo);
		
		
	}
}
