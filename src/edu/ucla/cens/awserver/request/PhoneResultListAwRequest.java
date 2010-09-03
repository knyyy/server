package edu.ucla.cens.awserver.request;

/**
 * Adds the phone version property to ResultListAwRequest.
 * 
 * @author selsky
 */
public class PhoneResultListAwRequest extends ResultListAwRequest {
	
	private String _phoneVersion;

	public String getPhoneVersion() {
		return _phoneVersion;
	}

	public void setPhoneVersion(String phoneVersion) {
		_phoneVersion = phoneVersion;
	}
}
