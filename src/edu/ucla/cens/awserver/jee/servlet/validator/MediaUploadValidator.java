package edu.ucla.cens.awserver.jee.servlet.validator;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.User;
import edu.ucla.cens.awserver.domain.UserImpl;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.MediaUploadAwRequest;

/**
 * Validates a multipart/form-data POST for media upload using the Apache Commons library. 
 * 
 * @author selsky
 */
public class MediaUploadValidator extends AbstractHttpServletRequestValidator {
	private static Logger _logger = Logger.getLogger(MediaUploadValidator.class);
	private Set<String> _parameterSet;
	private DiskFileItemFactory _diskFileItemFactory;
	
	public MediaUploadValidator(DiskFileItemFactory diskFileItemFactory) {
		if(null == diskFileItemFactory) {
			throw new IllegalArgumentException("a DiskFileItemFactory is required");
		}
		_diskFileItemFactory = diskFileItemFactory;
		
		_parameterSet = new TreeSet<String>();
		_parameterSet.addAll(Arrays.asList(new String[]{"c","ci","i","p","u"}));
	}
	
	/**
	 * Validates the media upload data and creates a new AwRequest in order to avoid parsing the multipart upload twice. 
	 * The AwRequest is pushed into the HttpServletRequest, which is a crappy hack that should be fixed. TODO
	 */
	@Override
	public boolean validate(HttpServletRequest request) {
		
		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(_diskFileItemFactory);
		upload.setHeaderEncoding("UTF-8");
		
		List uploadedItems = null;
		
		// Parse the request
		try {
		
			uploadedItems = upload.parseRequest(request);
		
		} catch(FileUploadException fue) {
			
			_logger.error("caught FileUploadException", fue);
			throw new IllegalStateException(fue);
		}
		
		int numberOfUploadedItems = uploadedItems.size();
		
		if(6 != numberOfUploadedItems) {
			_logger.warn("an incorrect number of parameters was found on media upload. 6 were expeced and " + numberOfUploadedItems
				+ " were received");
			return false;
		}
		
		AwRequest awRequest = new MediaUploadAwRequest();
		User user = new UserImpl();
		
		for(int i = 0; i < numberOfUploadedItems; i++) {
			FileItem fi = (FileItem) uploadedItems.get(i);
			
			if(fi.isFormField()) {
				String name = fi.getFieldName();
				if(! _parameterSet.contains(name)) {
					
					_logger.warn("an unknown parameter was found in a media upload: " + name);
					return false;
				}
				
				if("c".equals(name)) {
					
					if(greaterThanLength("campaign name", "c", fi.getString(), 750)) {
						return false;
					}
					
					user.setCurrentCampaignId(fi.getString());
				}
				
				if("ci".equals(name)) {
					
					if(greaterThanLength("client", "ci", fi.getString(), 600)) {
						return false;
					}
					
					awRequest.setClient(fi.getString());
				}
				
				if("i".equals(name)) {
					
					if(greaterThanLength("id", "i", fi.getString(), 36)) {
						return false;
					}
					
					awRequest.setMediaId(fi.getString());
				}
				
				if("p".equals(name)) {
					
					if(greaterThanLength("password", "p", fi.getString(), 180)) {
						return false;
					}
					
					// _logger.debug("'" + fi.getString() + "'");
					
					String tmp = null;
					try {
					
						tmp = URLDecoder.decode(fi.getString("UTF-8"), "UTF-8");
					
					} catch(UnsupportedEncodingException uee) {
						
						_logger.error(uee.getMessage());
					}
					
					user.setPassword(tmp);
				}
				
				
				if("u".equals(name)) {
					
					if(greaterThanLength("user name", "u", fi.getString(), 750)) {
						return false;
					}
					
					user.setUserName(fi.getString());
				}
				
			} else { // it's an attached file 
				
				// The media data is not checked because its length is so variable and potentially huge.
				// The default setting for Tomcat is to disallow requests that are greater than 2MB, which may have to change in the future
				
				String contentType = fi.getContentType();
				
				if(! "image/jpeg".equals(contentType)) { // only allow jpegs for now
					
					_logger.warn("an unsupported content-type was found for a media upload attachment: " + contentType);
					return false;
				}
				
				byte[] mediaBytes = fi.get();
				if(_logger.isDebugEnabled()) {
					_logger.debug("attempting upload of a file of " + mediaBytes.length + " bytes");
				}
				awRequest.setMedia(mediaBytes); // converts the stream to a byte array
			}
		}
		
		awRequest.setUser(user);
		request.setAttribute("awRequest", awRequest);
		return true;
	}
}
