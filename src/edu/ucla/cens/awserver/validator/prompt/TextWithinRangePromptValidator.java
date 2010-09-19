package edu.ucla.cens.awserver.validator.prompt;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;

/**
 * @author selsky
 */
public class TextWithinRangePromptValidator implements PromptValidator {
	
	/**
	 * Validates that the value from the PromptResponse is within the min and max specified by the Prompt. 
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		int min = Integer.parseInt(prompt.getProperties().get("min").getLabel());
		int max = Integer.parseInt(prompt.getProperties().get("max").getLabel());
		int length = ((String) promptResponse.getValue()).length();
		
		return length >= min && length <= max;
	}
}
