package edu.ucla.cens.awserver.domain;

import org.json.JSONObject;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * @author selsky
 */
public class MobilityModeFeaturesDataPacketBuilder extends AbstractDataPacketBuilder {

	public MobilityModeFeaturesDataPacketBuilder() {
		
	}
	
	public MetadataDataPacket createDataPacketFrom(JSONObject source, AwRequest awRequest) {
		MobilityModeFeaturesDataPacket packet = new MobilityModeFeaturesDataPacket();
		createCommonFields(source, packet);
		packet.setMode(JsonUtils.getStringFromJsonObject(source, "mode"));
		String featuresString = JsonUtils.getJsonObjectFromJsonObject(source, "features").toString();
		packet.setFeaturesString(featuresString);
		return packet;
	}
}
