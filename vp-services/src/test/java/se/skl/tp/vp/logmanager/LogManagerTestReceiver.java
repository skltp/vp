package se.skl.tp.vp.logmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogManagerTestReceiver {

	private static final Logger log = LoggerFactory.getLogger(LogManagerTestReceiver.class);

	public void process(Object message) {
		log.info("LogManagerTestReceiver received the message: {}", message);
	}
}
