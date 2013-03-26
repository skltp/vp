/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.io.UnsupportedEncodingException;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;

public class ResetVagvalCache implements Callable {

	private VisaVagvalsInterface vagvalAgent;
	
	// Not private to make the method testable...
	public void setVagvalAgent(VisaVagvalsInterface vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	public Object onCall(final MuleEventContext eventContext) throws Exception {
		eventContext.getMessage().clearProperties(PropertyScope.INVOCATION);

		final String content = getContentAndSetResponseContentType(eventContext);

		setResponseContentLength(eventContext, content);

		eventContext.setStopFurtherProcessing(true);

		return content;
	}

	private String getContentAndSetResponseContentType(final MuleEventContext eventContext)
			throws Exception {

		// Set some return info
		eventContext.getMessage().setProperty("Content-Type", "text/html; charset=" + eventContext.getEncoding(), PropertyScope.INBOUND);

		// Reset cache
		ResetVagvalCacheResponse result = vagvalAgent.resetVagvalCache(null);
		
		// Return success of operation
		return getResultAsString(result);
	}

	private void setResponseContentLength(final MuleEventContext eventContext, final String content)
			throws UnsupportedEncodingException {

		eventContext.getMessage().setProperty("Content-Length",
				Integer.toString(content.getBytes(eventContext.getEncoding()).length), PropertyScope.INBOUND);
	}
	
	private String getResultAsString(ResetVagvalCacheResponse result) {
		return "Vagval cache reset result = " + result.isResetResult();
	}
}
