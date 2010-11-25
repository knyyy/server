package edu.ucla.cens.awserver.domain;


/**
 * Domain object representing a mobility mode_features data packet.
 * 
 * @author selsky
 */
public class MobilityModeFeaturesDataPacket extends MobilityModeOnlyDataPacket {
	private String _featuresString;

	public String getFeaturesString() {
		return _featuresString;
	}

	public void setFeaturesString(String featuresString) {
		_featuresString = featuresString;
	}

	@Override
	public String toString() {
		return "MobilityModeFeaturesDataPacket [_featuresString="
				+ _featuresString + ", toString()=" + super.toString() + "]";
	}
}
