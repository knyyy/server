package edu.ucla.cens.awserver.dao;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

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
 * This class expects and imposes a structure on the media storage directory it uses (the fileSystemRootLoc argument to the 
 * constructor). On startup, it will look for the first subdirectory it can find in fileSystemLoc/000/000 with less than
 * 1000 files in it. If no subdirectory can be found, it will look in the parent dir for an available subdirectory (and then within
 * the grandparent dir if necessary) and create one at the next available name (e.g., 001). This (poorly explained) strategy will
 * manage a directory tree of 1 billion files. 
 * 
 * When this class creates directories, it relies on the OS for persmissions set up.
 * 
 * @author selsky
 */
public class UrlBasedResourceDao extends AbstractUploadDao {
	private Object lock = new Object();
	private Pattern _numberRegexp = Pattern.compile("[0-9]");
	private static Logger _logger = Logger.getLogger(UrlBasedResourceDao.class);
	private File _currentWriteDir;
	private String _currentFileName;
	private String _fileExtension;
	private static final String _insertSql = "insert into url_based_resource (user_id, uuid, url, client) values (?,?,?,?)";
	
	/**
	 * Creates an instance that uses the provided DataSource for database access, the rootDirectory as the root for filesystem
	 * storage, and the fileExtension for naming saved files with the appropriate extension. The rootDirectory is used to 
	 * create the initial directory for storage: rootDirectory/000/000/000. 
	 */
	public UrlBasedResourceDao(DataSource dataSource, String rootDirectory, String fileExtension) {
		super(dataSource);
		if(StringUtils.isEmptyOrWhitespaceOnly(rootDirectory)) {
			throw new IllegalArgumentException("rootDirectory is required");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(fileExtension)) {
			throw new IllegalArgumentException("fileExtension is required");
		}
		_fileExtension = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;
		init(rootDirectory);
	}
	
	/**
	 * Persists media data to a filesystem location and places a URL to the data into the url_based_resource table. Both tasks
	 * are handled together in order to deal with synchronizing creation of the storage URL (the subdirectory where the file is
	 * stored can change depending on whether it is full or not). The database write and filesystem write are handled as one
	 * transaction in order to deal with duplicate uploads.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		
		synchronized(lock) {
		
			if(_logger.isDebugEnabled()) {
				_logger.debug("saving a media file to the filesystem and a reference to it in url_based_resource");
			}
			
			final int userId = awRequest.getUser().getId();
			final String client = awRequest.getClient();
			final String uuid = awRequest.getMediaId();
			
			final String url = "file://" + _currentWriteDir + "/" + _currentFileName + _fileExtension;
			
			if(_logger.isDebugEnabled()) {
				_logger.debug("url to file: " + url);
			}
			
			OutputStream outputStream = null;
			
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
								ps.setString(4, client);
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
					
					outputStream = new BufferedOutputStream(new FileOutputStream(f));
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
					
					// ok, now set the write directory and file name for the next file
					setNextDirAndFile();
					
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
					
				} finally {
					if(null != outputStream) {
						try {
							
							outputStream.close();
							outputStream = null;
							
						} catch (IOException ioe) {
							
							_logger.error("caught IOException trying to close an output stream", ioe);
						}
					}
				}
			
			} catch(TransactionException te) {
				
				_logger.error("caught TransactionException when attempting to run the SQL + '" + _insertSql + "' with the following "
					+ "params: " + userId + ", " + uuid + ", " + url, te);
				rollback(transactionManager, status); // attempt to rollback even though the exception was thrown at the transaction level
				throw new DataAccessException(te);
			}
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
	
	private void init(String rootDir) { // e.g., /opt/aw/userdata/images
		synchronized(lock) {
			
			File rootDirectory = new File(rootDir);
			
			if(! rootDirectory.isDirectory()) {
				throw new IllegalArgumentException(rootDir + " is not a directory");
			}
			
			File f = null;
			
			if(rootDir.endsWith("/")) {
				f = new File(rootDirectory.getAbsolutePath() + "000/000/000");
			} else {
				f = new File(rootDirectory.getAbsolutePath() + "/000/000/000");
			}
			
			if(! f.mkdirs()) { 
				
				throw new IllegalStateException("cannot create " + f.getAbsolutePath() 
					+ " some of the intermediate dirs may have been created");
			}
			
			_currentWriteDir = f;
			_currentFileName = "000";
		}
	}
	
	/**
	 * Sets up the file name and directory for the next file to be written. This method contains no internal synchronization
	 * and relies on the synchronized block in execute().
	 */
	private void setNextDirAndFile() throws IOException {
		
		if(! isDirectoryFull(_currentWriteDir)) { // this is the same as "999".equals(_currentFileName)
			
			_currentFileName = incrementName(_currentFileName);
			
		} else {
			
			if("999".equals(_currentWriteDir.getName())) { // parent directory is also full
				
				File parentDir = _currentWriteDir.getParentFile();
				
				if("999".equals(parentDir.getName())) { // bad!!! the whole subtree is full. if this happens, it means there 
					                                    // have been manual changes on the filesystem or we have Petabytes of data
					                                    // i.e., we should run out of disk before this occurs
					
					_logger.fatal("media storage filesystem subtree is full!");
					throw new IOException("media storage filesystem subtree is full!");
					
				}
				
				_currentWriteDir = incrementDir(_currentWriteDir);
				_currentFileName = "000";
				
			} else {
				
				_currentWriteDir = incrementDir(_currentWriteDir);
				_currentFileName = "000";
			}
		}
	}
	
	private boolean isDirectoryFull(File dir) {
		NumberFileNameFilter filter = new NumberFileNameFilter(); // this can be an instance var
		if(dir.listFiles(filter).length < 1000) {
			return false;
		}
		return true;
	}
	
	private String incrementName(String name) {
		String s = String.valueOf(Integer.parseInt(name) + 1); // Integer.parseInt will gracefully truncate leading zeros
		int len = s.length();
		int zeroPadSize = 3 - len;
		if(zeroPadSize == 2){
			return "00" + s;
		} else if(zeroPadSize == 1) {
			return "0" + s;
		}
		return s;
	}
	
	private File incrementDir(File dir) throws IOException {
		String path = dir.getAbsolutePath();
		path = path.substring(0, path.lastIndexOf("/") + 1);
		
		String name = incrementName(dir.getName());
		File f = new File(path + name);
		if(! f.mkdir()) {
			throw new IOException("cannot make directory: " + f.getAbsolutePath());
		}
		return f;
	}
	
	public class DirectoryFilter implements FilenameFilter {
		public boolean accept(File f, String name) {
			return f.isDirectory() && _numberRegexp.matcher(name).matches();
		}
	}
	
	public class NumberFileNameFilter implements FilenameFilter {
		public boolean accept(File f, String name) {
			return _numberRegexp.matcher(name).matches() && ! f.isDirectory();
		}
	}
}
