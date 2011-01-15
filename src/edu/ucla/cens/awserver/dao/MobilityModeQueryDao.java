package edu.ucla.cens.awserver.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import edu.ucla.cens.awserver.domain.MobilityQueryResult;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.MobilityDataPointQueryAwRequest;

/**
 * @author selsky
 */
public class MobilityModeQueryDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(MobilityModeQueryDao.class);
	
	private String _sql = "SELECT mode, msg_timestamp, phone_timezone, latitude, longitude"
			            + " FROM mobility_mode_only_entry m, user u"
		                + " WHERE u.login_id = ?"
		                + " AND m.user_id = u.id"
		                + " AND m.msg_timestamp BETWEEN ? and ?";
	
	public MobilityModeQueryDao(DataSource dataSource) {
		super(dataSource);
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		MobilityDataPointQueryAwRequest req = (MobilityDataPointQueryAwRequest) awRequest;
		
		List<Object> paramObjects = new ArrayList<Object>();
		paramObjects.add(req.getUserNameRequestParam());
		paramObjects.add(req.getStartDate());
		paramObjects.add(req.getEndDate());
		
		try {
			
			List<?> results = getJdbcTemplate().query(_sql, paramObjects.toArray(), new MobilityModeQueryRowMapper());
			_logger.info("found " + results.size() + " query results");
			req.setResultList(results);
			
		} catch(org.springframework.dao.DataAccessException dae) {
			
			_logger.error("caught DataAccessException when running the following SQL '" + _sql + "' with the parameters: " +
				paramObjects, dae);
			
			throw new DataAccessException(dae);
		}
	}
	
	public class MobilityModeQueryRowMapper implements RowMapper {
	
		public Object mapRow(ResultSet rs, int index) throws SQLException {
			MobilityQueryResult result = new MobilityQueryResult();
			result.setMode(rs.getString(1));
			result.setTimestamp(rs.getString(2));
			result.setTimezone(rs.getString(3));
			result.setLatitude(rs.getDouble(4));
			result.setLongitude(rs.getDouble(5));
			return result;
		}
	}
}
