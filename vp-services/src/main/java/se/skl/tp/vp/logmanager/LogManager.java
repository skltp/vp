package se.skl.tp.vp.logmanager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEntryType;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;
import org.soitoolkit.commons.logentry.schema.v1.LogMessageType;
import org.soitoolkit.commons.logentry.schema.v1.LogMetadataInfoType;
import org.soitoolkit.commons.logentry.schema.v1.LogRuntimeInfoType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


public class LogManager implements Callable {

	private final static Logger log = LoggerFactory.getLogger(LogManager.class);
	
	private final static String INSERT_REQUEST = "INSERT INTO request " +
			"(senderId, contract, riv_version, receiver) " +
			"VALUES (?, ?, ?, ?)";
		
	
	private SimpleJdbcTemplate jdbcTemplate;
	
	private String senderIdPropertyName;
	private Pattern pattern;
	
	public void setDataSource(final DataSource dataSource) {
		log.info("DataSource set.");
		this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	public void setSenderIdPropertyName(String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		pattern = Pattern.compile(senderIdPropertyName + "=([^,]+)");
	}
	
	public synchronized LogEvent storeLogEventInDatabase(final LogEvent logEvent, final MuleMessage muleMessage) throws Exception {
		
		final String logState = logEvent.getLogEntry().getMessageInfo().getMessage();
		String msgId = logEvent.getLogEntry().getRuntimeInfo().getMessageId();
		log.info("LOG STATE: {}", logState);
		
		
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
			final String insertWaypoint = "insert into waypoint (log_state, timestamp, input_xml, response_xml, response_xml_producer, request_id) value (" +
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
		if (split.length != 5) {
			throw new IllegalArgumentException("Path to endpoint does not follow standard. " + ctxPath);
		}
		
		final String contract = split[2];
		final String rivVersion = split[4];
		
		/*
		 * Get senderId
		 */
		final String senderId = "";
		//final String senderId = VPUtil.getSenderIdFromCertificate(muleMessage, this.pattern);
		//log.debug("SENDER_ID: {}", senderId);
		
		/*
		 * Get receiver (riv:LogicalAddress or wsa:To)
		 */
		final String receiver = "";
		
		
		/*
		 * Find out whether we must insert a new main
		 * record
		 */
		try {
			final String find = "select request_id from request where request_id=?";
			final String result = this.jdbcTemplate.queryForObject(find, String.class, msgId);
		} catch (final DataAccessException e) {
			/*
			 * Insert new request record
			 */
			final String insertRequest = "insert into request (request_id, sender_id, riv_version, contract, receiver, timestamp) values (?,?,?,?,?,?)";
			int update = this.jdbcTemplate.update(insertRequest, new Object[] {msgId, 3L, rivVersion, contract, "12345", new Date(System.currentTimeMillis())});
			if (update != 1) {
				log.warn("Could not insert main request entry.");
			}
		}
		
		return msgId;
	}
	
	boolean mainRequestExists(final String msgId) {
		log.info("CHECKING IF MAIN REQUEST EXISTS...");
		try {
			final String sql = "select request_id from request where request_id=?";
			final String result = this.jdbcTemplate.queryForObject(sql, String.class, msgId);
			
			log.info("RESULT: {}", result == null ? false : true);
			return result == null ? false : true;
			
		} catch (final Exception e) {
			log.info("NON EXISTING");
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
