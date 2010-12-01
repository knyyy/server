package edu.ucla.cens.awserver.jee.servlet.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.domain.DataPointQueryResult;
import edu.ucla.cens.awserver.domain.DataPointQueryResultComparator;
import edu.ucla.cens.awserver.domain.ErrorResponse;
import edu.ucla.cens.awserver.request.AwRequest;

/**
 * @author selsky
 */
public class DataPointQueryResponseWriter extends AbstractResponseWriter {
	private static Logger _logger = Logger.getLogger(DataPointQueryResponseWriter.class);
	
	public DataPointQueryResponseWriter(ErrorResponse errorResponse) {
		super(errorResponse);
	}
	
	
	@Override
	public void write(HttpServletRequest request, HttpServletResponse response, AwRequest awRequest) {
		Writer writer = null;
		
		try {
			// Prepare for sending the response to the client
			writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(request, response)));
			String responseText = null;
			expireResponse(response);
			response.setContentType("application/json");
			JSONObject rootObject = new JSONObject();
			
			// Build the appropriate response 
			if(! awRequest.isFailedRequest()) {
				
				rootObject.put("result", "success");
				
				// Convert the results to JSON for output.
				@SuppressWarnings("unchecked")
				List<DataPointQueryResult> results =  (List<DataPointQueryResult>) awRequest.getResultList();
				Collections.sort(results, new DataPointQueryResultComparator()); // sort by surveyId and displayType so metadata
				                                                                 // entries are attached to the output correctly
				
				if(_logger.isDebugEnabled()) {
					_logger.debug(results);
				}
				
				JSONArray resultArray = new JSONArray();
				String currentSurveyId = results.size() > 0 ? results.get(0).getSurveyId() : null;
				JSONArray metadataArray = null;
				
				for(int i = 0; i < results.size(); i++) {
				
					// Every non-metadata promptId needs to have the metadata array for its survey attached to it 
					
					if(! currentSurveyId.equals(results.get(i).getSurveyId()) ) {
						currentSurveyId = results.get(i).getSurveyId();
						metadataArray = null;
					}
						
					DataPointQueryResult result = results.get(i);
					
					// The flow of this JSON serialization and the sort above rely on a few system facts:
					// 1. Only one dataPointId (promptId) can be queried at a time 
					// 2. All promptIds in a configuration are unique
					// 3. Any other promptIds in the results with the same surveyId will be metadata promptIds
					// 4. Queries against non-metadata promptIds are disallowed
					
					if(! "metadata".equals(result.getDisplayType())) {
						
						JSONObject entry = new JSONObject();
						
						if(null == metadataArray) {
							metadataArray = new JSONArray();
						}
						
						entry.put("metadata", metadataArray);
						entry.put("label", result.getPromptId()); // was result.getDisplayLabel()
						entry.put("value", result.getDisplayValue());
						
						if(null != result.getUnit()) {
							entry.put("unit", result.getUnit());
						}
						entry.put("timestamp", result.getTimestamp());
						entry.put("tz", result.getTimezone());
						entry.put("latitude", result.getLatitude());
						entry.put("longitude", result.getLongitude());
						entry.put("type", result.getDisplayType());
						if(null != result.getRepeatableSetIteration()) {
							entry.put("iteration", result.getRepeatableSetIteration());
						}
						
						resultArray.put(entry);
						
					} else { // now because of the sort all of the rest of the results for this survey id will be metadata 
						     // nodes
						
						JSONObject metadataEntry = new JSONObject();
						metadataEntry.put("id", result.getPromptId());
						metadataEntry.put("type", result.getPromptType());
						metadataEntry.put("value", result.getDisplayValue());
						metadataArray.put(metadataEntry);
					}
				}
				
				rootObject.put("data", resultArray);
				responseText = rootObject.toString();
				
			} else {
				
				if(null != awRequest.getFailedRequestErrorMessage()) {
					
					responseText = awRequest.getFailedRequestErrorMessage();
				
				} else {
				
					responseText = generalJsonErrorMessage();
				}
			}
			
			_logger.info("about to write output");
			writer.write(responseText);
		}
		
		catch(Exception e) { // catch Exception in order to avoid redundant catch block functionality (Java 7 will have 
			                 // comma-separated catch clauses) 
			
			_logger.error("an unrecoverable exception occurred while running an data point query", e);
			try {
				
				writer.write(generalJsonErrorMessage());
				
			} catch (Exception ee) {
				
				_logger.error("caught Exception when attempting to write to HTTP output stream", ee);
			}
			
		} finally {
			
			if(null != writer) {
				
				try {
					
					writer.flush();
					writer.close();
					writer = null;
					
				} catch (IOException ioe) {
					
					_logger.error("caught IOException when attempting to free resources", ioe);
				}
			}
		}
	}
}
