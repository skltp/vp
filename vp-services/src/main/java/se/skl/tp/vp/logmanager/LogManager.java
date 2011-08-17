package se.skl.tp.vp.logmanager;

import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.datatype.XMLGregorianCalendar;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType.ExtraInfo;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import se.skl.tp.vp.util.VPUtil;


public class LogManager implements Callable {

	private final static Logger log = LoggerFactory.getLogger(LogManager.class);
	
	private SimpleJdbcTemplate jdbcTemplate;
	
	public void setDataSource(final DataSource dataSource) {
		log.info("DataSource set.");
		this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	public synchronized LogEvent storeLogEventInDatabase(final LogEvent logEvent, final MuleMessage muleMessage) throws Exception {
		
		final Date ts = this.getDateFromLogEntry(logEvent);
		final String logState = logEvent.getLogEntry().getMessageInfo().getMessage();
		final String payload = logEvent.getLogEntry().getPayload();
		
		String msgId = logEvent.getLogEntry().getRuntimeInfo().getBusinessCorrelationId();
		
		/*
		 * Create main request if it's a new request
		 */
		boolean requestExists = this.mainRequestExists(msgId);
		if (!requestExists) {
			msgId = this.createMainRequestEntry(logEvent, muleMessage);
		}
		
		String rivVersion = null;
		final List<ExtraInfo> infos = logEvent.getLogEntry().getExtraInfo();
		for (final ExtraInfo info : infos) {
			if (info.getName().equalsIgnoreCase(VPUtil.RIV_VERSION)) {
				rivVersion = info.getValue();
				break;
			}
		}
		
		if (rivVersion == null) {
			rivVersion = "";
		}
		
		/*
		 * Insert waypoint
		 */
		if (msgId != null) {
			final String insertWaypoint = "insert into session_waypoint (waypoint, timestamp, payload, session_id, riv_version) values (" +
					"?, ?, ?, ?, ?)";
			
			int waypoint = this.jdbcTemplate.update(insertWaypoint, new Object[] { logState, ts.getTime(), payload, msgId, rivVersion});
			if (waypoint == 1) {
				log.debug("Inserted waypoint entry for msgId {}", msgId);
			}
		} else {
			log.error("Could not insert waypoint record because no log request entry could be found. Log event is: {}", logEvent);
		}
		
		/*
		 * Handle update of receiver, sender etc
		 */
		log.debug("Updating main request values...");
		this.updateMainRequest(logEvent.getLogEntry(), msgId);
		log.debug("Done updating values.");
		
		return logEvent;
	}
	
	void updateMainRequest(final LogEntryType msg, final String msgId) {
		this.updateField(VPUtil.SERVICE_NAMESPACE, "contract", msg, msgId);
		this.updateField(VPUtil.RECEIVER_ID, "receiver", msg, msgId);
		this.updateField(VPUtil.SENDER_ID, "sender_id", msg, msgId);
	}
	
	void updateField(final String item, final String column, final LogEntryType msg, final String msgId) {
		
		final String sql = "select " + column + " from session where session_id='" + msgId + "'";
		final String updateSql = "update session set " + column + " = ? where session_id = '" + msgId + "'";
		
		String value = null;
		for (final ExtraInfo s : msg.getExtraInfo()) {
			if (s.getName().equals(item)) {
				value = s.getValue();
				break;
			}
		}
		
		log.trace("Checking property {}. Value was {}.", new Object[] { item, value});
		if (value != null) {
			final String currentContract = this.jdbcTemplate.queryForObject(sql, String.class);
			
			if ((currentContract == null) || (!currentContract.equals(value)) ){
				log.trace("Updating main request value ({}). Previous: {}, New: {}", new Object[] {item, currentContract, value});
				this.jdbcTemplate.update(updateSql, value);
				
				return;
			}
		}
	}
	
	boolean isWhitespace(final String s) {
		if (s == null) {
			return true;
		}
		
		final String s2 = s.trim();
		return s2.length() == 0 && s2.equals("");
	}
	
	String createMainRequestEntry(final LogEvent logEvent, final MuleMessage msg) throws URISyntaxException {
		final LogEntryType logEntry = logEvent.getLogEntry();
		final LogRuntimeInfoType msgRt = logEntry.getRuntimeInfo();
		
		final String msgId = msgRt.getBusinessCorrelationId();
		
		final Date ts = this.getDateFromLogEntry(logEvent);
		
		/*
		 * Blank these fields initially and let them
		 * be updated by waypoint entries
		 */
		String senderId = "";
		String receiver = "";
		String contract = "";
		
		/*
		 * Find out whether we must insert a new main
		 * record
		 */
		try {
			final String find = "select session_id from session where session_id=?";
			this.jdbcTemplate.queryForObject(find, String.class, msgId);
		} catch (final DataAccessException e) {
			/*
			 * Insert new request record
			 */
			final String insertRequest = "insert into session (session_id, sender_id, contract, receiver, timestamp) values (?,?,?,?,?)";
			int update = this.jdbcTemplate.update(insertRequest, new Object[] {msgId, senderId, contract, receiver, ts.getTime()});
			if (update != 1) {
				log.warn("Could not insert main request entry.");
			}
		}
		
		return msgId;
	}
	
	boolean mainRequestExists(final String msgId) {
		try {
			final String sql = "select session_id from session where session_id=?";
			final String result = this.jdbcTemplate.queryForObject(sql, String.class, msgId);
			
			return result == null ? false : true;
		} catch (final Exception e) {
			return false;
		}
		
	}
	
	Date getDateFromLogEntry(final LogEvent le) {
		final XMLGregorianCalendar timestamp = le.getLogEntry().getRuntimeInfo().getTimestamp();
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, timestamp.getHour());
		c.set(Calendar.MINUTE, timestamp.getMinute());
		c.set(Calendar.SECOND, timestamp.getSecond());
		c.set(Calendar.MILLISECOND, timestamp.getMillisecond());
		c.set(Calendar.YEAR, timestamp.getYear());
		c.set(Calendar.MONTH, timestamp.getMonth());
		c.set(Calendar.DAY_OF_MONTH, timestamp.getDay());
		return c.getTime();
	}

	@Override
	public Object onCall(MuleEventContext arg0) throws Exception {
		final Object payload = arg0.getMessage().getPayload();
		log.debug("Log manager received request about storing log event into database");
		
		this.storeLogEventInDatabase((LogEvent) payload, arg0.getMessage());
		return payload;
	}
	
}
