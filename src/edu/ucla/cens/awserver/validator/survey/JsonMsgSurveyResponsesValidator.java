package edu.ucla.cens.awserver.validator.survey;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.cache.CacheService;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.JsonUtils;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;
import edu.ucla.cens.awserver.validator.json.AbstractAnnotatingJsonObjectValidator;

/**
 * Validator for the set of prompt responses in a survey. A survey is sent to the server as a JSON array of prompt or repeatable set
 * objects. Each object must be validated against its configuration. A prompt object contains a prompt_id and a value. A repeatable
 * set contains a repeatable_set_id, a skipped element, and an array of prompt_id-value (object) arrays. Multi-choice and 
 * single-choice prompts may be custom types in which case both a selected value (or values for multi-choice) and the set from which
 * the value was chosen from (custom_choices) are uploaded. This class is the main driver for validating each prompt or repeatable
 * set response. Validation is performed against a cached configuration identified by the combination of campaign name, campaign 
 * version, and survey id.   
 * 
 * @author selsky
 */
public class JsonMsgSurveyResponsesValidator extends AbstractAnnotatingJsonObjectValidator {
	private static Logger _logger = Logger.getLogger(JsonMsgSurveyResponsesValidator.class);
	private String _key = "responses";
	private CacheService _cacheService;
		
	public JsonMsgSurveyResponsesValidator(AwRequestAnnotator awRequestAnnotator, CacheService cacheService) {
		super(awRequestAnnotator);
		if(null == cacheService) {
			throw new IllegalArgumentException("a CacheService is required");
		}
		_cacheService = cacheService;
	}
	
	/**
	 * Validates each prompt response in a survey upload. Assumes the provided JSONObject contains one survey.
	 * 
	 * @return true if each response is conformant with its configuration
	 * @return false otherwise
	 */
	public boolean validate(AwRequest awRequest, JSONObject jsonObject) {		 
		JSONArray jsonArray = JsonUtils.getJsonArrayFromJsonObject(jsonObject, _key);
		int arraySize = jsonArray.length();
	
		for(int i = 0; i < arraySize; i++) {
			JSONObject response = JsonUtils.getJsonObjectFromJsonArray(jsonArray, i);
			
			// determine whether it is a repeatable set or a prompt response
			String promptId = JsonUtils.getStringFromJsonObject(response, "prompt_id");
			String repeatableSetId = null;
			if(null == promptId) {
				repeatableSetId = JsonUtils.getStringFromJsonObject(response, "repeatable_set_id");
				
				if(null == repeatableSetId) { // the response is malformed
					getAnnotator().annotate(awRequest, "malformed response: missing prompt_id and repeatable_set_id: "
						+ "one must be present");
					return false;
				}
				
				// handle the repeatable set
				
				
			} else {
				
				// handle the prompt
				// make sure the id exists
				
				
				
				
			}
		}
		
		return true;
	}
}
