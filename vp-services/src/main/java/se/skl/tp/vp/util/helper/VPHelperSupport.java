package se.skl.tp.vp.util.helper;

import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VPHelperSupport {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private MuleMessage muleMessage;
	private Pattern pattern;
	private String whiteList;
	
	public VPHelperSupport(final MuleMessage muleMessage, final Pattern pattern, final String whiteList) {
		this.muleMessage = muleMessage;
		this.pattern = pattern;
		this.whiteList = whiteList;
	}
	
	public MuleMessage getMuleMessage() {
		return this.muleMessage;
	}
	
	public Pattern getPattern() {
		return this.pattern;
	}
	
	public String getWhiteList() {
		return this.whiteList;
	}
	
	protected Logger getLog() {
		return this.log;
	}
}
