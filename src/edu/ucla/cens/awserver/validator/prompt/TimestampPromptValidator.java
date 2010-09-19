package edu.ucla.cens.awserver.validator.prompt;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;

/**
 * @author selsky
 */
public class TimestampPromptValidator implements PromptValidator {
	private static Logger _logger = Logger.getLogger(TimestampPromptValidator.class);
	
	/**
	 * Validates that the PromptResponse contains a timestamp of the form yyyy-MM-ddThh:mm:ss.
	 */
	@Override
	public boolean validate(Prompt prompt, PromptResponse promptResponse) {
		SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // the DateFormat classes are not threadsafe
		tsFormat.setLenient(false);
		String timestamp = (String) promptResponse.getValue();
		try {
			
			tsFormat.parse(timestamp);
			
		} catch (ParseException pe) {
			
			_logger.info("unparseable timestamp " + timestamp + " for prompt id " + prompt.getId());
			return false;
		}
		
		return false;
	}

}
