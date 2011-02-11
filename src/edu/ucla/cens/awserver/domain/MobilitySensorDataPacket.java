package edu.ucla.cens.awserver.domain;

import java.util.List;

import edu.ucla.cens.mobilityclassifier.Sample;

/**
 * Domain object representing a mobility sensor_data data packet.
 * 
 * @author selsky
 */
public class MobilitySensorDataPacket extends MobilityModeOnlyDataPacket {
	private String _sensorDataString;
	
	// Classifier output
	private List<Sample> _samples;
	private List<Double> _n95Fft;
	private List<Double> _fft;
	private Double _n95Variance;
	private Double _variance;
	private Double _average;
	private String _classifierMode;
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

	public List<Sample> getSamples() {
		return _samples;
	}

	public void setSamples(List<Sample> samples) {
		_samples = samples;
	}

	public List<Double> getN95Fft() {
		return _n95Fft;
	}

	public void setN95Fft(List<Double> n95Fft) {
		_n95Fft = n95Fft;
	}

	public List<Double> getFft() {
		return _fft;
	}

	public void setFft(List<Double> fft) {
		_fft = fft;
	}

	public Double getN95Variance() {
		return _n95Variance;
	}

	public void setN95Variance(Double n95Variance) {
		_n95Variance = n95Variance;
	}

	public Double getVariance() {
		return _variance;
	}

	public void setVariance(Double variance) {
		_variance = variance;
	}

	public Double getAverage() {
		return _average;
	}

	public void setAverage(Double average) {
		_average = average;
	}

	public String getClassifierMode() {
		return _classifierMode;
	}

	public void setClassifierMode(String classifierMode) {
		_classifierMode = classifierMode;
	}

	@Override
	public String toString() {
		return "MobilitySensorDataPacket [_average=" + _average
				+ ", _classifierMode=" + _classifierMode
				+ ", _classifierVersion=" + _classifierVersion + ", _fft="
				+ _fft + ", _n95Fft=" + _n95Fft + ", _n95Variance="
				+ _n95Variance + ", _samples=" + _samples
				+ ", _sensorDataString=" + _sensorDataString + ", _variance="
				+ _variance + "]";
	}
}
