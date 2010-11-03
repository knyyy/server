package edu.ucla.cens.awserver.jee.servlet.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.domain.ErrorResponse;
import edu.ucla.cens.awserver.request.AwRequest;

/**
 * @author selsky
 */
public class TokenAuthResponseWriter extends AbstractResponseWriter {
	private static Logger _logger = Logger.getLogger(TokenAuthResponseWriter.class);
	
	public TokenAuthResponseWriter(ErrorResponse errorResponse) {
		super(errorResponse);
	}
	
	/**
	 * Generates the JSON response to the client. For successful responses, an array of campaign names is returned.
	 */
	@Override
	public void write(HttpServletRequest request, HttpServletResponse response, AwRequest awRequest) {
		Writer writer = null;
		
		try {
			// Prepare for sending the response to the client
			writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(request, response)));
			String responseText = null;
			
			// Sets the HTTP headers to disable caching
			expireResponse(response);
			
			response.setContentType("application/json");
			
			// Build the appropriate response 
			if(! awRequest.isFailedRequest()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("result", "success");
				
				Set<String> keys = awRequest.getUser().getCampaignRoles().keySet();
				Iterator<String> iterator = keys.iterator();
				
				JSONArray jsonArray = new JSONArray();
					
				while(iterator.hasNext()) {
					jsonArray.put(iterator.next());
				}
				
				
				jsonObject.put("token", awRequest.getUserToken()); // this one line is the only difference with StatelessAuthResponseWriter
				
				jsonObject.put("campaigns", jsonArray);
				
				responseText = jsonObject.toString();
				
			} else {
				
				responseText = awRequest.getFailedRequestErrorMessage();
			}
			
			if(_logger.isDebugEnabled()) {
				_logger.debug("about to write output");
			}
			
			writer.write(responseText);
		}
		
		catch(Exception e) { // catch Exception in order to avoid redundant catch block functionality (Java 7 will have 
			                 // comma-separated catch clauses) 
			
			_logger.error("an unrecoverable exception occurred while attempting to serialize JSON", e);
			
			try {
				
				writer.write(this.generalJsonErrorMessage());
				
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
