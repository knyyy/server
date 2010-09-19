package edu.ucla.cens.awserver.validator.prompt;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;

/**
 * @author selsky
 */
public class SingleChoicePromptValidator implements PromptValidator {
	private static Logger _logger = Logger.getLogger(SingleChoicePromptValidator.class);
	
	/**
	 * Validates that the value within the PromptResponse is a valid single_choice key in the Prompt. 
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		String value = String.valueOf(promptResponse.getValue());
		Iterator<String> keySetIterator = prompt.getProperties().keySet().iterator();
		while(keySetIterator.hasNext()) {
			if(value.equals(keySetIterator.next())) {
				return true;
			}
		}
		
		if(_logger.isDebugEnabled()) {
			_logger.debug("single_choice value does not exist for prompt " + prompt.getId() + ". value: " + value);
		}
		
		return false;
	}

}
