package com.codewithjava21.weatherapp;

import com.google.gson.annotations.SerializedName;

public class AgentRequest {
	@SerializedName("input_value")
	private String inputValue;
	@SerializedName("output_type")
	private String outputType;
	@SerializedName("input_type")
	private String inputType;
	
	public AgentRequest(String message) {
		inputValue = message;
		outputType = "chat";
		inputType = "chat";
	}

	public String getInputValue() {
		return inputValue;
	}

	public void setInputValue(String inputValue) {
		this.inputValue = inputValue;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}
}
