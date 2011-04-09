/**
 * 
 */
package edu.ucla.cens.awserver.validator.prompt;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * Validator for the results from a RemoteActivity prompt type.
 * 
 * @author John Jenkins
 */
public class RemoteActivityResultSizePromptValidator extends AbstractPromptValidator {
	private static Logger _logger = Logger.getLogger(RemoteActivityResultSizePromptValidator.class);
	
	/**
	 * Validates that the result is a valid JSONArray and that the number of
	 * responses doesn't exceed that of the number of retries in the XML.
	 * 
	 * @see edu.ucla.cens.awserver.validator.prompt.PromptValidator#validate(edu.ucla.cens.awserver.domain.Prompt, org.json.JSONObject)
	 */
	@Override
	public boolean validate(Prompt prompt, JSONObject promptResponse) {
		_logger.debug("Recieved message");
		if(isNotDisplayed(prompt, promptResponse))
		{
			return true;
		}
		
		if(isSkipped(prompt, promptResponse))
		{
			return isValidSkipped(prompt, promptResponse);
		}
		
		JSONArray responseJsonArray = JsonUtils.getJsonArrayFromJsonObject(promptResponse, "value");
		if(responseJsonArray == null)
		{
			if(_logger.isDebugEnabled())
			{
				_logger.debug("Missing or invalid JSONArray for prompt " + prompt.getId());
			}
			return false;
		}
		_logger.debug("Uploaded response: " + responseJsonArray.toString());
		
		int numRetries = Integer.parseInt(prompt.getProperties().get("retries").getLabel());
		
		return(responseJsonArray.length() <= (numRetries + 1));
	}
}
