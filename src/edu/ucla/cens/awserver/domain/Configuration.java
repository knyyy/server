package edu.ucla.cens.awserver.domain;

/**
 * Immutable bean-style wrapper for configuration properties.
 * 
 * @author selsky
 */
public class Configuration {
	private String _campaignName;
	private String _campaignVersion;
	private String _xml;
	
	public Configuration(String campaignName, String campaignVersion, String xml) {
		if(null == campaignName) {
			throw new IllegalArgumentException("a campaignName is required");
		}
		if(null == campaignVersion) {
			throw new IllegalArgumentException("a campaignVersion is required");
		}
		if(null == xml) {
			throw new IllegalArgumentException("xml is required");
		}
		
		_campaignName = campaignName;
		_campaignVersion = campaignVersion;
		_xml = xml;
	}

	public String getCampaignName() {
		return _campaignName;
	}

	public String getCampaignVersion() {
		return _campaignVersion;
	}

	public String getXml() {
		return _xml;
	}

	@Override
	public String toString() {
		return "Configuration [_campaignName=" + _campaignName
				+ ", _campaignVersion=" + _campaignVersion + ", _xml=" + _xml
				+ "]";
	}
}
