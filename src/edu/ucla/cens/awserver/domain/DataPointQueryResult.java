package edu.ucla.cens.awserver.domain;

/**
 * @author selsky
 */
public class DataPointQueryResult {
	private String _response;
	private int _repeatableSetIteration;
	private double _latitude;
	private double _longitude;
	private String _timestamp;
	private String _timezone;
	private String _promptId;
	private String _promptType;
	private String _repeatableSetId;
	
	public String getResponse() {
		return _response;
	}
	
	public void setResponse(String response) {
		_response = response;
	}
	
	public int getRepeatableSetIteration() {
		return _repeatableSetIteration;
	}
	
	public void setRepeatableSetIteration(int repeatableSetIteration) {
		_repeatableSetIteration = repeatableSetIteration;
	}
	
	public double getLatitude() {
		return _latitude;
	}
	
	public void setLatitude(double latitude) {
		_latitude = latitude;
	}
	
	public double getLongitude() {
		return _longitude;
	}
	
	public void setLongitude(double longitude) {
		_longitude = longitude;
	}
	
	public String getTimestamp() {
		return _timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		_timestamp = timestamp;
	}
	
	public String getTimezone() {
		return _timezone;
	}
	
	public void setTimezone(String timezone) {
		_timezone = timezone;
	}
	
	public String getPromptId() {
		return _promptId;
	}
	
	public void setPromptId(String promptId) {
		_promptId = promptId;
	}
	
	public String getPromptType() {
		return _promptType;
	}
	
	public void setPromptType(String promptType) {
		_promptType = promptType;
	}
	
	public String getRepeatableSetId() {
		return _repeatableSetId;
	}
	
	public void setRepeatableSetId(String repeatableSetId) {
		_repeatableSetId = repeatableSetId;
	}

	@Override
	public String toString() {
		return "DataPointQueryResult [_latitude=" + _latitude + ", _longitude="
				+ _longitude + ", _promptId=" + _promptId + ", _promptType="
				+ _promptType + ", _repeatableSetId=" + _repeatableSetId
				+ ", _repeatableSetIteration=" + _repeatableSetIteration
				+ ", _response=" + _response + ", _timestamp=" + _timestamp
				+ ", _timezone=" + _timezone + "]";
	}
}
