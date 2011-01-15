package edu.ucla.cens.awserver.domain;

/**
 * @author selsky
 */
public class MobilityQueryResult {
	private Double _longitude;
	private Double _latitude;
	private String _mode;
	private String _timestamp;
	private String _timezone;
	
	public Double getLongitude() {
		return _longitude;
	}
	public void setLongitude(Double longitude) {
		_longitude = longitude;
	}
	public Double getLatitude() {
		return _latitude;
	}
	public void setLatitude(Double latitude) {
		_latitude = latitude;
	}
	public String getMode() {
		return _mode;
	}
	public void setMode(String mode) {
		_mode = mode;
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
}
