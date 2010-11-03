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

import edu.ucla.cens.awserver.domain.ErrorResponse;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.RetrieveConfigAwRequest;

/**
 * @author selsky
 */
public class RetrieveConfigResponseWriter extends AbstractResponseWriter {
	private static Logger _logger = Logger.getLogger(RetrieveConfigResponseWriter.class);
	private List<String> _dataPointApiSpecialIds;
	
	public RetrieveConfigResponseWriter(List<String> dataPointApiSpecialIds, ErrorResponse errorResponse) {
		super(errorResponse);
		if(null == dataPointApiSpecialIds || dataPointApiSpecialIds.isEmpty()) {
			_logger.warn("no data point API special ids found");
		}
		_dataPointApiSpecialIds = dataPointApiSpecialIds;  
	}
	
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
				
				JSONObject jsonObject = 
					new JSONObject().put("result", "success")
					                .put("configuration", ((RetrieveConfigAwRequest) awRequest).getOutputConfigXml().replaceAll("\\n", " "))
				                    .put("user_role", ((RetrieveConfigAwRequest) awRequest).getOutputUserRole())
				                    .put("user_list", new JSONArray(((RetrieveConfigAwRequest) awRequest).getOutputUserList()))
				                    // TODO add the special_ids when the Data Point API is written 
				                    // The special ids can be added one by one as the system makes the queries available
				                    .put("special_ids", new JSONArray(_dataPointApiSpecialIds));
				
				responseText = jsonObject.toString();
				
			} else {
				
				responseText = awRequest.getFailedRequestErrorMessage();
			}
			
			_logger.info("about to write output");
			writer.write(responseText);
		}
		
		catch(Exception e) { // catch Exception in order to avoid redundant catch block functionality
			
			_logger.error("an unrecoverable exception occurred while running an retrieve config query", e);
			
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
