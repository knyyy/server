package edu.ucla.cens.awserver.validator.prompt;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;

/**
 * @author selsky
 */
public class UUIDPromptValidator implements PromptValidator {
	private static Logger _logger = Logger.getLogger(UUIDPromptValidator.class);
	private static Pattern _pattern 
		= Pattern.compile("[a-fA-F0-9]{8}\\-[a-fA-F0-9]{4}\\-[a-fA-F0-9]{4}\\-[a-fA-F0-9]{4}\\-[a-fA-F0-9]{12}");
	
	/**
	 * Ignores the Prompt parameter and validates that the value in the PromptResponse is a correctly formed UUID.
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		// Example from UUID.randomUUID()
		// afda1b74-4f23-4068-a50b-664e1c347264
		
		if(! _pattern.matcher((String) promptResponse.getValue()).matches()) {
			if(_logger.isDebugEnabled()) {
				_logger.debug("invalid UUID for prompt " + prompt.getId() + ". value: " + promptResponse.getValue());
			}
			return false;
		}
		
		return false;
	}
	
//	public static void main(String args[]) {
//		System.out.println(_pattern.matcher("afda1b74-4f23-4068-a50b-664e1c347264").matches());
//	}

}
