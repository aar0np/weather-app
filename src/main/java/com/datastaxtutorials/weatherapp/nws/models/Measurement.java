package com.datastaxtutorials.weatherapp.nws.models;

public class Measurement {

	private String unitCode;
	private float value;
	
	public String getUnitCode() {
		return unitCode;
	}
	
	public void setUnitCode(String unitCode) {
		this.unitCode = unitCode;
	}
	
	public float getValue() {
		return value;
	}
	
	public void setValue(float value) {
		this.value = value;
	}
}
