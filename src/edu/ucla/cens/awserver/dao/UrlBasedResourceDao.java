package edu.ucla.cens.awserver.dao;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * DAO for saving a media resource (a byte array) to a filesystem location and pointing a row in the url_based_resource table to 
 * that filesystem location.
 * 
 * @author selsky
 */
public class UrlBasedResourceDao extends AbstractUploadDao {
	private static Logger _logger = Logger.getLogger(UrlBasedResourceDao.class);
	private String _fileSystemLoc;
	private String _fileSuffix;
	private static final String _insertSql = "insert into url_based_resource (user_id, uuid, url) values (?,?,?)";
	
	public UrlBasedResourceDao(DataSource dataSource, String fileSystemLoc, String fileSuffix) {
		super(dataSource);
		if(StringUtils.isEmptyOrWhitespaceOnly(fileSystemLoc)) {
			throw new IllegalArgumentException("fileSystemLoc is required");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(fileSuffix)) {
			throw new IllegalArgumentException("fileSuffix is required");
		}
		_fileSystemLoc = fileSystemLoc;
		_fileSuffix = fileSuffix;
	}
	
	/**
	 * Persists media data to a filesystem location and places a "pointer" to that filesystem data into the url_based_resource 
	 * table. This two-step process is handled here in one place in order to skip filesystem persistence if a duplicate is 
	 * found in the url_based_resource table.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("saving a media file to the filesystem and a reference to it in url_based_resource");
		}
		
		final int userId = awRequest.getUser().getId();
		final String uuid = awRequest.getMediaId();
		final String url = "file://" + _fileSystemLoc + "/" + uuid + _fileSuffix; 
		if(_logger.isDebugEnabled()) {
			_logger.debug("url to file: " + url);
		}
		
		// Wrap this upload in a transaction 
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("media upload");
		
		PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
		TransactionStatus status = transactionManager.getTransaction(def); // begin transaction
		
		// a Savepoint could be used here, but since there is only one row to be inserted a 
		// regular rollback() will do the trick.
		
		try {
			
			try {
				
				// first, save the id and location to the db
				getJdbcTemplate().update( 
					new PreparedStatementCreator() {
						public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
							PreparedStatement ps = connection.prepareStatement(_insertSql);
							ps.setInt(1, userId);
							ps.setString(2, uuid);
							ps.setString(3, url);
							return ps;
						}
					}
				);
				
				// ok, now save to the file system
				
				File f = new File(new URI(url));
				if(! f.createNewFile()) { // bad!! This means the file already exists, but there was no row for it in 
					                      // url_based_resource
					rollback(transactionManager, status);
					f = null;
					throw new DataAccessException("file already exists: " + url); 
				}
				
				BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(f));
				byte[] bytes = awRequest.getMedia();
				int length = bytes.length;
				int offset = 0;
				int writeLen = 1024;
				int total = 0;
				
				while(total < length) {
					int amountToWrite = writeLen < (length - total) ? writeLen : (length - total);
					outputStream.write(bytes, offset, amountToWrite);
					offset += amountToWrite;
					total += writeLen;
				}
				
				outputStream.close();
				transactionManager.commit(status);
				
			} catch (DataIntegrityViolationException dive) {
				
				if(isDuplicate(dive)) {
					
					if(_logger.isDebugEnabled()) {
						_logger.info("found a duplicate media upload message. uuid: " + uuid);
					}
					
					handleDuplicate(awRequest, 1); // 1 is passed here because there is only one media resource uploaded at a time
					rollback(transactionManager, status);
					
				} else {
				
					// some other integrity violation occurred - bad!! All of the data to be inserted must be validated
					// before this DAO runs so there is either missing validation or somehow an auto_incremented key
					// has been duplicated
					
					_logger.error("caught DataAccessException", dive);
					rollback(transactionManager, status);
					throw new DataAccessException(dive);
				}
				
			} catch (org.springframework.dao.DataAccessException dae) {
				
				_logger.error("caught DataAccessException when attempting to run the SQL + '" + _insertSql + "' with the following "
						+ "params: " + userId + ", " + uuid + ", " + url, dae);
				rollback(transactionManager, status);
				throw new DataAccessException(dae);
	
			} catch (IOException ioe) {
				
				_logger.error("caught IOException", ioe);
				rollback(transactionManager, status);
				throw new DataAccessException(ioe);

			} catch (URISyntaxException use) {
				
				_logger.error("caught URISyntaxException", use);
				rollback(transactionManager, status);
				throw new DataAccessException(use);
			}
		
		} catch(TransactionException te) {
			
			_logger.error("caught TransactionException when attempting to run the SQL + '" + _insertSql + "' with the following "
				+ "params: " + userId + ", " + uuid + ", " + url, te);
			rollback(transactionManager, status); // attempt to rollback even though the exception was thrown at the transaction level
			throw new DataAccessException(te);
		}
	}
	
	/**
	 * Attempts to rollback a transaction. 
	 */
	private void rollback(PlatformTransactionManager transactionManager, TransactionStatus transactionStatus) {
		
		try {
			
			_logger.error("rolling back a failed media upload transaction");
			transactionManager.rollback(transactionStatus);
			
		} catch (TransactionException te) {
			
			_logger.error("failed to rollback media upload transaction", te);
			throw new DataAccessException(te);
		}
	}
	
//	/**
//	 * 
//	 */
//	private void logErrorDetails(int userId, String uuid, String url) {
//		
//		_logger.error("an error occurred running the following SQL '" + _insertSql );
//	}
}
