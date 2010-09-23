package edu.ucla.cens.awserver.domain;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * @author selsky
 */
public class SurveyDataPacketBuilder extends AbstractDataPacketBuilder {
	private static Logger _logger = Logger.getLogger(SurveyDataPacketBuilder.class);
	
	/**
	 * Creates a SurveyDataPacket from a survey upload. Assumes that the upload message is valid.
	 */
	public DataPacket createDataPacketFrom(JSONObject source) {
		SurveyDataPacket surveyDataPacket = new SurveyDataPacket();
		createCommonFields(source, surveyDataPacket);
		surveyDataPacket.setSurveyId(JsonUtils.getStringFromJsonObject(source, "survey_id"));
		surveyDataPacket.setSurvey(JsonUtils.getJsonArrayFromJsonObject(source, "responses").toString());
		if(_logger.isDebugEnabled()) {
			_logger.debug(surveyDataPacket);
		}
		return surveyDataPacket;
	}
}
