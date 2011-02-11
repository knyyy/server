package edu.ucla.cens.awserver.service;

import java.util.List;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.DataPacket;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.mobilityclassifier.MobilityClassifier;

/**
 * @author selsky
 */
public class MobilityClassifierService implements Service {
	private static Logger _logger = Logger.getLogger(MobilityClassifierService.class);
	private MobilityClassifier _classifier;
	
	public MobilityClassifierService(MobilityClassifier classifier) {
		if(null == classifier) {
			throw new IllegalArgumentException("the MobilityClassifier cannot be null");
		}
		_classifier = classifier;
	}
	
	
	@Override
	public void execute(AwRequest awRequest) {
		List<DataPacket> dataPackets = awRequest.getDataPackets();
		
		
		
		

	}
}
