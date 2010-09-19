package edu.ucla.cens.awserver.validator.prompt;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;

/**
 * @author selsky
 */
public class RangeBoundNumberPromptValidator implements PromptValidator {
	private static Logger _logger = Logger.getLogger(RangeBoundNumberPromptValidator.class);
	
	/**
	 * Validates that the value in the PromptResponse is within the bounds set by the Prompt.
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		int min = Integer.parseInt(prompt.getProperties().get("min").getLabel());
		int max = Integer.parseInt(prompt.getProperties().get("max").getLabel());
		String value = (String) promptResponse.getValue();
		int v = 0;
		
		try {
			
			v = Integer.parseInt(value);
			
		} catch (NumberFormatException nfe) {
			
			_logger.debug("unparseable range-bound number value: " + value);
			return false;
		}
		
		return v >= min && v <= max;
	}

}
