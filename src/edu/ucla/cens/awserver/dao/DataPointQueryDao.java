package edu.ucla.cens.awserver.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import edu.ucla.cens.awserver.domain.DataPointQueryResult;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.DataPointQueryAwRequest;

/**
 * @author selsky
 */
public class DataPointQueryDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(DataPointQueryDao.class);
	
	private String _select = "SELECT pr.prompt_id, pr.prompt_type, pr.response, pr.repeatable_set_iteration, pr.repeatable_set_id,"
			            + " sr.msg_timestamp, sr.phone_timezone, sr.latitude, sr.longitude"
	                    + " FROM prompt_response pr, survey_response sr"
	                    + " WHERE pr.survey_response_id = sr.id"
	                    + " AND sr.msg_timestamp BETWEEN ? AND ?";
	
	private String _params = " AND prompt_id in (?";
	
	public DataPointQueryDao(DataSource dataSource) {
		super(dataSource);
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		DataPointQueryAwRequest req = (DataPointQueryAwRequest) awRequest;
		List<String> metadataPromptIds = req.getMetadataPromptIds();
		int numberOfMetadataPoints = metadataPromptIds.size(); 
		
		StringBuilder builder = new StringBuilder(_params);
		for(int i = 0; i < numberOfMetadataPoints; i++) {
			builder.append(",?");
		}
		builder.append(")");
		
		final String sql = _select + builder.toString();
		
		if(_logger.isDebugEnabled()) {
			_logger.debug(sql);
		}
		
		final List<Object> paramObjects = new ArrayList<Object>();
		paramObjects.add(req.getStartDate());
		paramObjects.add(req.getEndDate());
		paramObjects.add(req.getDataPointId());
		
		for(int i = 0; i < numberOfMetadataPoints; i++) {
			paramObjects.add(metadataPromptIds.get(i));
		}
		
		try {
		
			List<?> results = getJdbcTemplate().query(sql, paramObjects.toArray(), new DataPointQueryRowMapper());
			_logger.info("found " + results.size() + " query results");
			req.setResultList(results);
			
		} catch(org.springframework.dao.DataAccessException dae) {
			
			_logger.error("caught DataAccessException when running the following SQL '" + sql + "' with the parameters: " +
				req.getStartDate() + ", " + req.getEndDate() + ", " + req.getDataPointId() + ", " + paramObjects, dae);
			
			throw new DataAccessException(dae);
		}
	}
	
	public class DataPointQueryRowMapper implements RowMapper {
		
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			DataPointQueryResult result = new DataPointQueryResult();
			result.setPromptId(rs.getString(1));
			result.setPromptType(rs.getString(2));
			result.setResponse(rs.getString(3));
			Object o = rs.getObject(4);
			if(null == o) {
				result.setRepeatableSetIteration(-1);	
			} else {
				result.setRepeatableSetIteration(rs.getInt(4));
			}
			result.setRepeatableSetId(rs.getString(5));
			result.setTimestamp(rs.getString(6));
			result.setTimezone(rs.getString(7));
			result.setLatitude(rs.getDouble(8));
			result.setLongitude(rs.getDouble(9));
			return result;
		}
	}
}
