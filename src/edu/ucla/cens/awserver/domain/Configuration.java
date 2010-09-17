package edu.ucla.cens.awserver.domain;

import java.io.IOException;
import java.io.StringReader;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;

/**
 * Immutable bean-style wrapper for configuration properties.
 * 
 * @author selsky
 */
public class Configuration {
	private String _campaignName;
	private String _campaignVersion;
	private Document _xmlDocument;
	
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
		
		Builder builder = new Builder();
		
		try {
			
			_xmlDocument = builder.build(new StringReader(xml));
		} 
		catch(IOException ioe) {
			throw new IllegalStateException("could not read XML string", ioe);
		}
		catch(ParsingException pe) {
			throw new IllegalStateException("could not parse XML string", pe);
		}
	}

	public String getCampaignName() {
		return _campaignName;
	}

	public String getCampaignVersion() {
		return _campaignVersion;
	}

	public Document getXmlDocument() {
		return new Document(_xmlDocument); // return a defensive copy ensuring immutability
	}

	@Override
	public String toString() {
		return "Configuration [_campaignName=" + _campaignName
				+ ", _campaignVersion=" + _campaignVersion + ", _xml=" + _xmlDocument
				+ "]";
	}
}
