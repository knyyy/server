package edu.ucla.cens.awserver.request;


/**
 * Represents state for survey uploads.
 * 
 * @author selsky
 */
public class SurveyUploadAwRequest extends UploadAwRequest {
	private String _campaignVersion;

	/**
	 * Default no-arg constructor.	
	 */
	public SurveyUploadAwRequest() {
		super();
	}
	
	public String getCampaignVersion() {
		return _campaignVersion;
	}

	public void setCampaignVersion(String campaignVersion) {
		_campaignVersion = campaignVersion;
	}

	@Override
	public String toString() {
		return "SurveyUploadAwRequest [_campaignVersion=" + _campaignVersion
				+ ", toString()=" + super.toString() + "]";
	}
}

