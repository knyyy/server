package edu.ucla.cens.awserver.jee.servlet.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.domain.DataPointQueryResult;
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
				List<?> results =  awRequest.getResultList();
				
				JSONArray jsonArray = new JSONArray();
				
				for(int i = 0; i < results.size(); i++) {
					DataPointQueryResult result = (DataPointQueryResult) results.get(i);
					JSONObject entry = new JSONObject();	
					
					entry.put("label", result.getDisplayLabel());
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
					
					jsonArray.put(entry);
				}
				
				rootObject.put("data", jsonArray);
				responseText = rootObject.toString();
				
			} else {
				
				responseText = awRequest.getFailedRequestErrorMessage();
			}
			
			_logger.info("about to write output");
			writer.write(responseText);
		}
		
		catch(Exception e) { // catch Exception in order to avoid redundant catch block functionality (Java 7 will have 
			                 // comma-separated catch clauses) 
			
			_logger.error("an unrecoverable exception occurred while running an EMA query", e);
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
