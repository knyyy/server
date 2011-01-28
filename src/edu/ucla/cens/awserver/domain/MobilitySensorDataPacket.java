package edu.ucla.cens.awserver.domain;


/**
 * Domain object representing a mobility sensor_data data packet.
 * 
 * @author selsky
 */
public class MobilitySensorDataPacket extends MobilityModeOnlyDataPacket {
	private String _sensorDataString;
	private String _classifierVersion;
	
	public String getSensorDataString() {
		return _sensorDataString;
	}

	public void setSensorDataString(String sensorDataString) {
		_sensorDataString = sensorDataString;
	}

	public String getClassifierVersion() {
		return _classifierVersion;
	}

	public void setClassifierVersion(String classifierVersion) {
		_classifierVersion = classifierVersion;
	}

	@Override
	public String toString() {
		return "MobilitySensorDataPacket [_classifierVersion="
				+ _classifierVersion + ", _sensorDataString="
				+ _sensorDataString + "]";
	}
}
