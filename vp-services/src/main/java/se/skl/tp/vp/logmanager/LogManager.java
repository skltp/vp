package se.skl.tp.vp.logmanager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType.ExtraInfo;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageType;
import org.soitoolkit.commons.logentry.schema.v1.LogMetadataInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


public class LogManager implements Callable {

	private final static Logger log = LoggerFactory.getLogger(LogManager.class);
	
	private SimpleJdbcTemplate jdbcTemplate;
	
	public void setDataSource(final DataSource dataSource) {
		log.info("DataSource set.");
		this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	public synchronized LogEvent storeLogEventInDatabase(final LogEvent logEvent, final MuleMessage muleMessage) throws Exception {
		
		final String logState = logEvent.getLogEntry().getMessageInfo().getMessage();
		String msgId = logEvent.getLogEntry().getRuntimeInfo().getMessageId();
		
		/*
		 * Create main request if it's a new request
		 */
		boolean requestExists = this.mainRequestExists(msgId);
		if (!requestExists) {
			msgId = this.createMainRequestEntry(logEvent, muleMessage);
		}
		
		/*
		 * Insert waypoint
		 */
		if (msgId != null) {
			final String insertWaypoint = "insert into session_waypoint (waypoint, timestamp, request_xml, response_xml, response_xml_producer, session_id) values (" +
					"?, ?, ?, ?, ?, ?)";
			
			int waypoint = this.jdbcTemplate.update(insertWaypoint, new Object[] { logState, new Date(System.currentTimeMillis()), null, null, null, msgId});
			if (waypoint == 1) {
				log.info("Inserted waypoint entry for msgId {}", msgId);
			}
		} else {
			log.error("Could not insert waypoint record because no log request entry could be found. Log event is: {}", logEvent);
		}
		
		return logEvent;
	}
	
	String createMainRequestEntry(final LogEvent logEvent, final MuleMessage msg) throws URISyntaxException {
		final LogEntryType logEntry = logEvent.getLogEntry();
		final LogMessageType msgType = logEntry.getMessageInfo();
		final LogMetadataInfoType msgMeta = logEntry.getMetadataInfo();
		final LogRuntimeInfoType msgRt = logEntry.getRuntimeInfo();
		
		final String endpoint = msgMeta.getEndpoint();
		final String msgId = msgRt.getMessageId();
		final String logState = msgType.getMessage();
		
		final URI uri = new URI(endpoint);
		final String ctxPath = uri.getPath();
		
		final String[] split = ctxPath.split("/");
		
		/*
		 * [0] =
		 * [1] = vp
		 * [2] = contract
		 * [3] = version
		 * [4] = riv_version
		 */
		String contract = "";
		String rivVersion = "";
		if (split.length == 5) {
			contract = split[2];
			rivVersion = split[4];
		}
		
		/*
		 * Get senderId
		 */
		String senderId = "";
		final List<ExtraInfo> infos = logEntry.getExtraInfo();
		for (final ExtraInfo ei : infos) {
			if (ei.getName().equals("senderId")) {
				senderId = ei.getValue();
			}
		}
		log.debug("SENDER_ID: {}", senderId);
		
		/*
		 * Get receiver (riv:LogicalAddress or wsa:To)
		 */
		final String receiver = "";
		
		
		/*
		 * Find out whether we must insert a new main
		 * record
		 */
		try {
			final String find = "select session_id from session where session_id=?";
			final String result = this.jdbcTemplate.queryForObject(find, String.class, msgId);
		} catch (final DataAccessException e) {
			/*
			 * Insert new request record
			 */
			final String insertRequest = "insert into session (session_id, sender_id, riv_version, contract, receiver, timestamp) values (?,?,?,?,?,?)";
			int update = this.jdbcTemplate.update(insertRequest, new Object[] {msgId, senderId, rivVersion, contract, "12345", new Date(System.currentTimeMillis())});
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

	@Override
	public Object onCall(MuleEventContext arg0) throws Exception {
		final Object payload = arg0.getMessage().getPayload();
		log.info("Log manager received request about storing log event into database");
		
		this.storeLogEventInDatabase((LogEvent) payload, arg0.getMessage());
		return payload;
	}
	
}
