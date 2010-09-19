package edu.ucla.cens.awserver.validator.prompt;

import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;
import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * @author selsky
 */
public class SingleChoiceCustomPromptValidator extends AbstractCustomChoicePromptValidator {
	private static Logger _logger = Logger.getLogger(SingleChoiceCustomPromptValidator.class);
	
	/**
	 * Validates that the PromptResponse contains a value (an Integer) that matches a response's custom_choices. For custom prompts, 
	 * the PromptResponse contains both the prompt's configuration (custom_choices) and the associated values the user chose. In 
	 * addition to validating the values a user choice, the configuration must also be validated.
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		if(! (promptResponse.getValue() instanceof JSONObject)) {
			_logger.warn("Malformed single_choice_custom message. Expected a " + prompt.getId() + " response"
				+ " (PromptResponse.getValue()) to return a JSONObject and it returned " + promptResponse.getValue().getClass());
			return false;
		}
		
		JSONObject object = (JSONObject) promptResponse.getValue();
		Integer value = JsonUtils.getIntegerFromJsonObject(object, "value");
		if(null == value) {
			_logger.warn("Malformed single_choice_custom message. Missing value for response for " + prompt.getId());
			return false;
		}
		
		JSONArray choices = JsonUtils.getJsonArrayFromJsonObject(object, "custom_choices");
		if(null == choices) {
			_logger.warn("Malformed single_choice_custom message. Missing custom_choices for response for " + prompt.getId());
			return false;
		}
		
		Set<Integer> choiceSet = validateCustomChoices(choices, prompt);
		if(null == choiceSet) {
			return false;
		}
			
		if(! choiceSet.contains(value)) {
			_logger.warn("Malformed single_choice_custom message. Unknown choice value for " + prompt.getId());
			return false;
		}
		
		return true;
	}

}
