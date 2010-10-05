package edu.ucla.cens.awserver.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import edu.ucla.cens.awserver.domain.DataPacket;
import edu.ucla.cens.awserver.domain.MobilityModeFeaturesDataPacket;
import edu.ucla.cens.awserver.domain.MobilityModeOnlyDataPacket;
import edu.ucla.cens.awserver.request.AwRequest;


/**
 * DAO for handling persistence of uploaded mobility mode_only data. 
 * 
 * @author selsky
 */
public class MobilityUploadDao extends AbstractUploadDao {
	private static Logger _logger = Logger.getLogger(MobilityUploadDao.class);

	private final String _insertMobilityModeOnlySql = "insert into mobility_mode_only_entry" +
	                                                  " (user_id, msg_timestamp, epoch_millis, phone_timezone, latitude," +
	                                                  " longitude, accuracy, provider, mode) values (?,?,?,?,?,?,?,?,?) ";

	private final String _insertMobilityModeFeaturesSql = "insert into mobility_mode_features_entry" +
			                                              " (user_id, msg_timestamp, epoch_millis, phone_timezone, latitude," +
			                                              " longitude, accuracy, provider, mode, speed, variance, average, fft)" +
			                                              " values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	public MobilityUploadDao(DataSource datasource) {
		super(datasource);
	}
	
	/**
	 * Attempts to insert mobility DataPackets into the db. If any duplicates are found, they are simply logged. For mobility
	 * uploads, it is possible to receive both types (mode_only and mode_features) within one set of messages, so this method 
	 * handles them both.
	 * 
	 * @throws DataAccessException if any errors other than a duplicate record occur
	 * @throws IllegalArgumentException if a List of DataPackets is not present as an attribute on the AwRequest
	 */
	public void execute(AwRequest awRequest) {
		_logger.info("beginning to persist mobility messages");
		
		List<DataPacket> dataPackets = awRequest.getDataPackets();
		
		if(null == dataPackets) {
			throw new IllegalArgumentException("no DataPackets found in the AwRequest");
		}
		
		int userId = awRequest.getUser().getId();
		int index = -1;
		DataPacket currentDataPacket = null;
		
		// Wrap this upload in a transaction 
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("mobility upload");
		
		PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
		TransactionStatus status = transactionManager.getTransaction(def); // begin transaction
		
		// Use a savepoint to handle nested rollbacks if duplicates are found
		Object savepoint = status.createSavepoint();
		
		boolean isModeFeatures = false;
		
		try { // handle TransactionExceptions
			
			try { // handle DataAccessExceptions
				
				for(DataPacket dataPacket : dataPackets) {
						
					currentDataPacket = dataPacket;
					
					index++;
					int numberOfRowsUpdated = 0;
					
					if(dataPacket instanceof MobilityModeFeaturesDataPacket) { // the order of these instanceofs is important because
						                                                       // a MobilityModeFeaturesDataPacket is a 
						                                                       // MobilityModeOnlyDataPacket -- need to check for the 
						                                                       // superclass first -- maybe move away from instanceof?
						isModeFeatures = true;
						numberOfRowsUpdated = insertMobilityModeFeatures((MobilityModeFeaturesDataPacket)dataPacket, userId);
						
					} else if (dataPacket instanceof MobilityModeOnlyDataPacket){
						
						numberOfRowsUpdated = insertMobilityModeOnly((MobilityModeOnlyDataPacket)dataPacket, userId);
						
					} else { // this is a logical error because this class should never be called with non-mobility packets
						
						throw new IllegalArgumentException("invalid data packet found: " + dataPacket.getClass());
					}
					
					if(1 != numberOfRowsUpdated) {
						throw new DataAccessException("inserted multiple rows even though one row was intended. sql: " 
								+  (isModeFeatures ? _insertMobilityModeFeaturesSql : _insertMobilityModeOnlySql)); 
					}
					
					savepoint = status.createSavepoint();
				}
				
			} catch (DataIntegrityViolationException dive) { 
					
				if(isDuplicate(dive)) {
					
					if(_logger.isDebugEnabled()) {
						_logger.info("found a duplicate mobility message");
					}
					handleDuplicate(awRequest, index);
					status.rollbackToSavepoint(savepoint);
					
				} else {
				
					// some other integrity violation occurred - bad!! All of the data to be inserted must be validated
					// before this DAO runs so there is either missing validation or somehow an auto_incremented key
					// has been duplicated
					
					logError(isModeFeatures, currentDataPacket, userId);
					rollback(transactionManager, status, isModeFeatures, currentDataPacket, userId);
					throw new DataAccessException(dive);
				}
				
			} catch (org.springframework.dao.DataAccessException dae) { // some other database problem happened that prevented
				                                                        // the SQL from completing normally
				
				logError(isModeFeatures, currentDataPacket, userId);
				rollback(transactionManager, status, isModeFeatures, currentDataPacket, userId);
				throw new DataAccessException(dae);
				
			}
		
			transactionManager.commit(status);
			_logger.info("completed mobility message persistence");	
		
		} catch (TransactionException te) { 
			
			_logger.error("failed to commit mobility upload transaction, attempting to rollback", te);
			rollback(transactionManager, status, isModeFeatures, currentDataPacket, userId);
			logError(isModeFeatures, currentDataPacket, userId);
			throw new DataAccessException(te);
		}
	}
	
	/**
	 * Attempts to rollback a transaction. 
	 */
	private void rollback(PlatformTransactionManager transactionManager, TransactionStatus transactionStatus, 
			boolean isModeFeatures, DataPacket dataPacket, int userId) {
		
		try {
			
			_logger.info("rolling back a failed mobility upload transaction");
			transactionManager.rollback(transactionStatus);
			
		} catch (TransactionException te) {
			
			_logger.error("failed to rollback mobility upload transaction", te);
			logError(isModeFeatures, dataPacket, userId);
			throw new DataAccessException(te);
		}
	}
	
	/**
	 * Insert a row into mobility_mode_only_entry. 
	 */
	private int insertMobilityModeOnly(final MobilityModeOnlyDataPacket dataPacket, final int userId) { 
		
		return getJdbcTemplate().update( 
			new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(_insertMobilityModeOnlySql);
					ps.setInt(1, userId);
					ps.setTimestamp(2, Timestamp.valueOf(dataPacket.getDate()));
					ps.setLong(3, dataPacket.getEpochTime());
					ps.setString(4, dataPacket.getTimezone());
					
					if(dataPacket.getLatitude().isNaN()) {
						ps.setNull(5, Types.DOUBLE);
					} else {
						ps.setDouble(5, dataPacket.getLatitude());
					}
					
					if(dataPacket.getLongitude().isNaN()) { 
						ps.setNull(6, Types.DOUBLE);
					} else {
						ps.setDouble(6, dataPacket.getLongitude());
					}
										
					ps.setDouble(7, dataPacket.getAccuracy());
					ps.setString(8, dataPacket.getProvider());
					ps.setString(9, dataPacket.getMode());
					
					return ps;
				}
			}
		); 
	}
	
	/**
	 * Insert a row into mobility_mode_features_entry. 
	 */
	private int insertMobilityModeFeatures(final MobilityModeFeaturesDataPacket dataPacket, final int userId) {
		
		return getJdbcTemplate().update( 
			new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement(_insertMobilityModeFeaturesSql);
					ps.setInt(1, userId);
					ps.setTimestamp(2, Timestamp.valueOf(dataPacket.getDate()));
					ps.setLong(3, dataPacket.getEpochTime());
					ps.setString(4, dataPacket.getTimezone());
					
					if(dataPacket.getLatitude().isNaN()) {
						ps.setNull(5, Types.DOUBLE);
					} else {
						ps.setDouble(5, dataPacket.getLatitude());
					}
					
					if(dataPacket.getLongitude().isNaN()) { 
						ps.setNull(6, Types.DOUBLE);
					} else {
						ps.setDouble(6, dataPacket.getLongitude());
					}
					
					
					ps.setDouble(7, dataPacket.getAccuracy());
					ps.setString(8, dataPacket.getProvider());
					ps.setString(9, dataPacket.getMode());
					ps.setDouble(10, dataPacket.getSpeed());
					ps.setDouble(11, dataPacket.getVariance());
					ps.setDouble(12, dataPacket.getAverage());
					ps.setString(13, dataPacket.getFftArray());
											
					return ps;
				}
			}
		);
	}
	
	private void logError(boolean isModeFeatures, DataPacket dataPacket, int userId) {
		
		if(isModeFeatures) {
			
			MobilityModeFeaturesDataPacket mmfdp = (MobilityModeFeaturesDataPacket) dataPacket;
			
			_logger.error("caught DataAccessException when running SQL '" + _insertMobilityModeFeaturesSql + "' with the following" +
				" parameters: " + userId + ", " + Timestamp.valueOf(mmfdp.getDate()) + ", " + mmfdp.getEpochTime() + ", " +
				mmfdp.getTimezone() + ", " + (mmfdp.getLatitude().equals(Double.NaN) ? "null" : mmfdp.getLatitude()) +  ", " + 
			    (mmfdp.getLongitude().equals(Double.NaN) ? "null" : mmfdp.getLongitude()) + ", " + mmfdp.getAccuracy() + ", " +
			    mmfdp.getProvider() + ", " +  mmfdp.getMode() + ", " + 
			    mmfdp.getSpeed() + ", " + mmfdp.getVariance() + ", " + mmfdp.getAverage() + ", " + mmfdp.getFftArray());
			 
		} else {
			
			MobilityModeOnlyDataPacket mmodp = (MobilityModeOnlyDataPacket) dataPacket;

			_logger.error("caught DataAccessException when running SQL '" + _insertMobilityModeOnlySql +"' with the following " +
				"parameters: " + userId + ", " + mmodp.getDate() + ", " + mmodp.getEpochTime() + ", " + mmodp.getTimezone() + ", "
				+ (mmodp.getLatitude().equals(Double.NaN) ? "null" : mmodp.getLatitude()) + ", " + 
				(mmodp.getLongitude().equals(Double.NaN) ? "null" : mmodp.getLongitude()) + ", " + mmodp.getAccuracy() + " ," + 
				mmodp.getProvider() + " ," + mmodp.getMode());
			
		}
	}
}
