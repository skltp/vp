package se.skl.tp.vp.util.helper;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VPHelperSupport {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private MuleMessage muleMessage;
	
	public VPHelperSupport(final MuleMessage muleMessage) {
		this.muleMessage = muleMessage;
	}
	
	public MuleMessage getMuleMessage() {
		return this.muleMessage;
	}
	
	protected Logger getLog() {
		return this.log;
	}
}
