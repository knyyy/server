package edu.ucla.cens.awserver.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import edu.ucla.cens.awserver.domain.DataPacket;
import edu.ucla.cens.awserver.domain.PromptResponseDataPacket;
import edu.ucla.cens.awserver.domain.SurveyDataPacket;
import edu.ucla.cens.awserver.request.AwRequest;

/**
 * Persist surveys to the db: entire surveys are persisted as JSON. For individual prompt response persistence, see
 * SurveyResponsesUploadDao.
 * 
 * @author selsky
 */
public class SurveyUploadDao extends AbstractUploadDao {
	private static Logger _logger = Logger.getLogger(SurveyUploadDao.class);
	
	private final String _selectCampaignConfigId = "select id from campaign_configuration where campaign_id = ? and version = ?";
	
	private final String _insertSurveyResponse = "insert into survey_response" +
								           		 " (user_id, campaign_configuration_id, time_stamp, epoch_millis, phone_timezone," +
									 	         " latitude, longitude, accuracy, provider, survey_id, json) " +
										         " values (?,?,?,?,?,?,?,?,?,?,?)";
	
	private final String _insertPromptResponse = "insert into prompt_response" +
	                                             " (user_id, survey_response_id, repeatable_set_id, prompt_type, prompt_id," +
	                                             " response)" +
	                                             " values (?,?,?,?,?,?)";
	
	public SurveyUploadDao(DataSource dataSource) {
		super(dataSource);
	}
	
	/**
	 * Inserts surveys and prompts into the survey_response and prompt_response tables. Expects the surveys to be in the form of
	 * DataPackets where each survey contains a list of prompt response DataPackets as well.
	 * 
	 * TODO the inserts in this method must be made transactional
	 */
	@Override
	public void execute(AwRequest awRequest) {
		final int campaignConfigurationId;
		
		try {
			campaignConfigurationId = getJdbcTemplate().queryForInt(
				_selectCampaignConfigId, 
				new Object[] {awRequest.getUser().getCurrentCampaignId(), awRequest.getCampaignVersion()}
			);
		}
		catch (IncorrectResultSizeDataAccessException irsdae) { // this means that no rows were returned on the SQL returned more 
			                                                    // than one column -- either way, there is a logical error
			_logger.error("cannot retrieve campaign_configuration.id -- SQL [" + _selectCampaignConfigId 
				+ "] returned no rows or multiple columns", irsdae);
			throw new DataAccessException(irsdae);
		}
		catch(org.springframework.dao.DataAccessException dae) {
			_logger.error(dae.getMessage(), dae);
			throw new DataAccessException(dae); 
		}
		
		// TODO need a transaction here !
		
		List<DataPacket> surveys = awRequest.getDataPackets(); // each survey is JSON stored as a String
		final int userId = awRequest.getUser().getId();
		int numberOfSurveys = surveys.size();
		int surveyIndex = 0;
		boolean persistingSurvey = false;
		SurveyDataPacket currentSurveyDataPacket = null;
		PromptResponseDataPacket currentPromptResponseDataPacket = null;
		int currentSurveyResponseId = -1;
		
		try {
			for(; surveyIndex < numberOfSurveys; surveyIndex++) {
				
				final SurveyDataPacket surveyDataPacket = (SurveyDataPacket) surveys.get(surveyIndex);
				currentSurveyDataPacket = surveyDataPacket; // this is annoying, but the currentSurveyDataPacket is used for 
				                                            // logging purposes outside of the try/catch because the locally final 
				                                            // surveyDataPacket cannot be defined as final outside of the loop's 
				                                            // scope. this "current" variable strategy is used below as well for the
				                                            // same reason.
				
				KeyHolder idKeyHolder = new GeneratedKeyHolder();
				persistingSurvey = true;
				
				// First, insert the survey
				
				getJdbcTemplate().update(
					new PreparedStatementCreator() {
						public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
							PreparedStatement ps 
								= connection.prepareStatement(_insertSurveyResponse, Statement.RETURN_GENERATED_KEYS);
							ps.setInt(1, userId);
							ps.setInt(2, campaignConfigurationId);
							ps.setTimestamp(3, Timestamp.valueOf(surveyDataPacket.getDate()));
							ps.setLong(4, surveyDataPacket.getEpochTime());
							ps.setString(5, surveyDataPacket.getTimezone());
							if(surveyDataPacket.getLatitude().isNaN()) {
								ps.setNull(6, Types.DOUBLE);
							} else {
								ps.setDouble(6, surveyDataPacket.getLatitude());
							}
							if(surveyDataPacket.getLongitude().isNaN()) { 
								ps.setNull(7, Types.DOUBLE);
							} else {
								ps.setDouble(7, surveyDataPacket.getLongitude());
							}
							ps.setDouble(8, surveyDataPacket.getAccuracy());
							ps.setString(9, surveyDataPacket.getProvider());
							ps.setString(10, surveyDataPacket.getSurveyId());
							ps.setString(11, surveyDataPacket.getSurvey());
							
							return ps;
						}
					},
					idKeyHolder
				);
				
				final Number surveyResponseId = idKeyHolder.getKey(); // the primary key on the survey_response table for the 
				                                                      // just-inserted survey
				currentSurveyResponseId = surveyResponseId.intValue();
				
				// Now insert each prompt response from the survey
				
				List<PromptResponseDataPacket> promptResponseDataPackets = surveyDataPacket.getResponses();
				for(int i = 0; i < promptResponseDataPackets.size(); i++) {
					persistingSurvey = false;
					final PromptResponseDataPacket prdp = promptResponseDataPackets.get(i);	
					currentPromptResponseDataPacket = prdp;
					
					getJdbcTemplate().update(
						new PreparedStatementCreator() {
							public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
								PreparedStatement ps 
									= connection.prepareStatement(_insertPromptResponse);
								ps.setInt(1, userId);
								ps.setInt(2, surveyResponseId.intValue());
								ps.setString(3, prdp.getRepeatableSetId());
								ps.setString(4, prdp.getType());
								ps.setString(5, prdp.getPromptId());
								ps.setString(6, prdp.getValue());
								
								return ps;
							}
						}
					);
				}
			}
			
		} catch (DataIntegrityViolationException dive) { // a unique index exists only on the survey_response table 
				
			if(isDuplicate(dive)) {
				
				if(_logger.isDebugEnabled()) {
					_logger.info("found a duplicate survey upload message");
				}
				
				// handleDuplicate(awRequest, surveyIndex, currentSurveyDataPacket, userId);
				handleDuplicate(awRequest, surveyIndex);
				
			} else {
			
				// some other integrity violation occurred - bad!! All of the data to be inserted must be validated
				// before this DAO runs so there is either missing validation or somehow an auto_incremented key
				// has been duplicated
				
				logError(persistingSurvey, currentSurveyDataPacket, userId, campaignConfigurationId, currentSurveyResponseId);
				throw new DataAccessException(dive);
			}
				
		} catch (org.springframework.dao.DataAccessException dae) { // some other database problem happened that prevented
			                                                        // the SQL from completing normally
			
			logError(persistingSurvey, currentPromptResponseDataPacket, userId, campaignConfigurationId, currentSurveyResponseId);
			throw new DataAccessException(dae);
		}
	}
	
	private void logError(boolean persistingSurvey, DataPacket dp, int userId, int campaignConfigurationId, int surveyResponseId) {

		if(persistingSurvey) {
			
			SurveyDataPacket sdp = (SurveyDataPacket) dp;
			
			_logger.error("caught DataAccessException when running SQL '" + _insertSurveyResponse + "' with the following "
				+ "parameters: " + userId + ", " + campaignConfigurationId + ", " + sdp.getDate() + " , " + sdp.getEpochTime()
				+  ", " + sdp.getTimezone() + ", " + sdp.getLatitude() + ", " + sdp.getLongitude() + ", " + sdp.getAccuracy() 
				+ ", " + sdp.getProvider() + ", " + sdp.getSurveyId() + sdp.getSurvey());
			
		} else {
			
			PromptResponseDataPacket prdp = (PromptResponseDataPacket) dp;
			
			_logger.error("caught DataAccessException when running SQL '" + _insertPromptResponse + "' with the following "
				+ "parameters: " + userId + ", " + surveyResponseId + ", " + prdp.getRepeatableSetId() + ", " + prdp.getType() 
				+ ", " + prdp.getPromptId() + ", " + prdp.getValue());
		}
	}
	
//	private void handleDuplicate(AwRequest awRequest, int surveyIndex, SurveyDataPacket surveyDataPacket, int userId) {
//		handleDuplicate(awRequest, surveyIndex);
//		
//		List<DuplicateSurveyUpload> dupes = awRequest.getDuplicateSurveyUploads();
//		if(null == dupes) {
//			dupes = new ArrayList<DuplicateSurveyUpload>();
//			awRequest.setDuplicateSurveyUploads(dupes);
//		}
//		
//		dupes.add(new DuplicateSurveyUpload(userId, surveyDataPacket.getEpochTime(), surveyDataPacket.getSurveyId()));
//	}
}
