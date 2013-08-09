/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.dashboard;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

public class HtmlDashboard implements Callable, Initialisable {

	private int refreshPeriod = 60;

	private HtmlDashboardRenderer htmlDashboardRenderer;

	private CssProvider cssProvider;

	public void setRefreshPeriod(final int refreshPeriod) {
		this.refreshPeriod = refreshPeriod;
	}
	
	public void initialise() throws InitialisationException {
		try {
			htmlDashboardRenderer = new HtmlDashboardRenderer(refreshPeriod);
			cssProvider = new CssProvider();
		} catch (final UnknownHostException uhe) {
			throw new InitialisationException(uhe, this);
		} catch (final IOException ioe) {
			throw new InitialisationException(ioe, this);
		}
	}

	public Object onCall(final MuleEventContext eventContext) throws Exception {
		eventContext.getMessage().clearProperties();

		final String content = getContentAndSetResponseContentType(eventContext);

		setResponseContentLength(eventContext, content);

		eventContext.setStopFurtherProcessing(true);

		return content;
	}

	@SuppressWarnings("unchecked")
	private String getContentAndSetResponseContentType(final MuleEventContext eventContext)
			throws Exception {

		if (StringUtils.contains(eventContext.getMessage().getPayloadAsString(), "css")) {

			eventContext.getMessage().setStringProperty("Content-Type",
					"text/css; charset=" + eventContext.getEncoding());

			return cssProvider.getContent();
		}

		eventContext.getMessage().setStringProperty("Content-Type",
				"text/html; charset=" + eventContext.getEncoding());

		// Set statistics object in the htmlDashboardRender
		htmlDashboardRenderer.setStatistics((Map<String, ServiceStatistics>) eventContext
				.getMuleContext().getRegistry().lookupObject("tp-statistics"));

		return htmlDashboardRenderer.getContent();
	}

	private void setResponseContentLength(final MuleEventContext eventContext, final String content)
			throws UnsupportedEncodingException {

		eventContext.getMessage().setStringProperty("Content-Length",
				Integer.toString(content.getBytes(eventContext.getEncoding()).length));
	}

	
}
