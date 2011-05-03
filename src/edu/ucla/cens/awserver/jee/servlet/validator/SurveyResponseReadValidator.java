package edu.ucla.cens.awserver.jee.servlet.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * Validator for inbound data to the "new" data point API.
 * 
 * @author selsky
 */
public class SurveyResponseReadValidator extends AbstractHttpServletRequestValidator {
	private static Logger _logger = Logger.getLogger(SurveyResponseReadValidator.class);
	private List<String> _parameterList;
	
	public SurveyResponseReadValidator() {
		_parameterList = new ArrayList<String>(Arrays.asList(new String[]{"start_date",
				                                                          "end_date",
				                                                          "user_list",
				                                                          "campaign_urn",
				                                                          "client",
				                                                          "prompt_id_list",
				                                                          "auth_token",
				                                                          "survey_id_list",
				                                                          "column_list",
				                                                          "output_format",
				                                                          "pretty_print",
	    																  "suppress_metadata"}));
	}
	
	public boolean validate(HttpServletRequest httpServletRequest) {
		Map<String,String[]> parameterMap = getParameterMap(httpServletRequest); 
		
		int totalNumberOfParameters = _parameterList.size();
		int numberOfQueryParameters = parameterMap.size();
		
		// Check for missing or extra parameters
		if((numberOfQueryParameters != (totalNumberOfParameters - 1))       // either prompt_ids or survey_ids is required, but not both,
		    && (numberOfQueryParameters != (totalNumberOfParameters - 2))   // and pretty_print, suppress-metadata, start_date, and end_date
			&& (numberOfQueryParameters != (totalNumberOfParameters - 3))   // are optional
			&& (numberOfQueryParameters != (totalNumberOfParameters - 5))) {
			
			_logger.warn("an incorrect number of parameters was found for /app/survey_response/read: " + numberOfQueryParameters);
			return false;
		}
		
		// Check for duplicate parameter values (except for "i")
		Iterator<?> iterator = parameterMap.keySet().iterator();
		
		while(iterator.hasNext()) {
			String key = (String) iterator.next();
			String[] valuesForKey = (String[]) parameterMap.get(key);
			
			if(valuesForKey.length != 1) {
				_logger.warn("an incorrect number of values (" + valuesForKey.length + ") was found for parameter " + key);
				return false;
			}
		}
		
		// Check for parameters with unknown names
		if(containsUnknownParameter(parameterMap, _parameterList)) {
			return false;
		}
		
		String startDate = (String) httpServletRequest.getParameter("start_date");
		String endDate = (String) httpServletRequest.getParameter("end_date");
		String users = (String) httpServletRequest.getParameter("user_list");
		String campaignUrn = (String) httpServletRequest.getParameter("campaign_urn");
		String client = (String) httpServletRequest.getParameter("client");
		String token = (String) httpServletRequest.getParameter("auth_token");
		String promptIds = (String) httpServletRequest.getParameter("prompt_id_list");
		String surveyIds = (String) httpServletRequest.getParameter("survey_id_list");
		String columns = (String) httpServletRequest.getParameter("column_list");
		String outputFormat = (String) httpServletRequest.getParameter("output_format");
		String prettyPrint = (String) httpServletRequest.getParameter("pretty_print");
		String suppressMetadata = (String) httpServletRequest.getParameter("suppress_metadata");
		
		// Check for abnormal lengths (buffer overflow attack)
		
		if(greaterThanLength("startDate", "start_date", startDate, 10) 
		   || greaterThanLength("endDate", "end_date", endDate, 10)
		   || greaterThanLength("campaignUrn", "campaign_urn", campaignUrn, 250)
		   || greaterThanLength("client", "client", client, 250)		   
		   || greaterThanLength("authToken", "token", token, 36)
		   || greaterThanLength("users", "user_list", users, 150) // allows up to 10 users
		   || greaterThanLength("promptIdList", "prompt_id_list", promptIds, 2500)  // arbitrary, but longer than this would be abnormal
		   || greaterThanLength("surveyIdlist", "survey_id_list", surveyIds, 2500)  // arbitrary, but longer than this would be abnormal 
		   || greaterThanLength("columnList", "column_list", columns, 2500)         // arbitrary, but longer than this would be abnormal
		   || greaterThanLength("outputFormat", "output_format", outputFormat, 12)  // longest value allowed is "json-columns" 
		   || greaterThanLength("prettyPrint", "pretty_print", prettyPrint, 5)      // longest value allowed is "false"
		   || greaterThanLength("suppressMetadata", "suppress_metadata", suppressMetadata, 5)) { // longest value allowed is "false"
			
			_logger.warn("found an input parameter that exceeds its allowed length");
			return false;
		}
		
		return true;
	}
}