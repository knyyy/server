package edu.ucla.cens.awserver.validator.prompt;

import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;
import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * @author selsky
 */
public class MultiChoicePromptValidator implements PromptValidator {
	private static Logger _logger = Logger.getLogger(MultiChoicePromptValidator.class);
	
	/**
	 * Validates that the value from the PromptResponse (a JSON array) contains valid keys from the Prompt.
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		JSONArray jsonArray = (JSONArray) promptResponse.getValue();
		Set<String> keySet = prompt.getProperties().keySet();
		
		for(int i = 0; i < jsonArray.length(); i++) {
			String selection = JsonUtils.getStringFromJsonArray(jsonArray, i); //TODO will the JSON lib auto-convert ints to strings?
			if(! keySet.contains(selection)) { 
				
				if(_logger.isDebugEnabled()) {
					_logger.debug("invalid multi_choice selection " + selection + ". prompt id: " + prompt.getId());
				}
				
				return false;
			}
		}
		
		return true;
	}
}
