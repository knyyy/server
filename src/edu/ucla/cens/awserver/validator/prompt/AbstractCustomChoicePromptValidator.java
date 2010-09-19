package edu.ucla.cens.awserver.validator.prompt;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.util.JsonUtils;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * @author selsky
 */
public abstract class AbstractCustomChoicePromptValidator implements PromptValidator {
	private static Logger _logger = Logger.getLogger(AbstractCustomChoicePromptValidator.class);
	
	/**
	 * Returns a set of integers representing valid choice keys if the custom_choices JSON fragment is semantically well-formed.
	 */
	protected Set<Integer> validateCustomChoices(JSONArray choices, Prompt prompt) {
		// Validate the choice keys
		int numberOfCustomChoices = choices.length();
		Set<Integer> choiceSet = new HashSet<Integer>();
		
		for(int i = 0; i < numberOfCustomChoices; i++) {
			JSONObject choiceObject = JsonUtils.getJsonObjectFromJsonArray(choices, i);
			if(null == choiceObject) {
				_logger.warn("Malformed custom choice message. Expected a JSONObject at custom_choices index " 
					+ i + "  for " + prompt.getId());
				return null;
			}
			
			Integer choiceKey = JsonUtils.getIntegerFromJsonObject(choiceObject, "choice_id");
			if(null == choiceKey) {
				_logger.warn("Malformed custom choice message. Expected a choice_id at custom_choices index " 
						+ i + "  for " + prompt.getId());
				return null;
			}
			
			// make sure there are also values, duplicates allowed (TODO - is that correct??)
			String choiceValue = JsonUtils.getStringFromJsonObject(choiceObject, "choice_value");
			if(StringUtils.isEmptyOrWhitespaceOnly(choiceValue)) {
				_logger.warn("Malformed custom choice message. Expected a choice_value at custom_choices index " 
						+ i + "  for " + prompt.getId());
				return null;
			}
			
			if(! choiceSet.add(choiceKey)) {
				_logger.warn("duplicate custom_choice found for prompt " + prompt.getId() + " custom_choices: " + choices);
				return null;
			}
		}
		
		return choiceSet;
	}
}
