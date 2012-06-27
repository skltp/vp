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
package se.skl.tp.vp.dashboard;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;

class HtmlDashboardRenderer {

	private final int refreshPeriod;
	
	private Map<String, ServiceStatistics> statistics;

	private final String hostName;

	public HtmlDashboardRenderer(final int refreshPeriod)
			throws UnknownHostException {
		hostName = InetAddress.getLocalHost().getHostName();
		this.refreshPeriod = refreshPeriod;
	}

	public void setStatistics(Map<String, ServiceStatistics> statistics) {
		this.statistics = statistics;
	}

	public String getContent() {
		final StringBuilder htmlBuilder = new StringBuilder();

		renderHeader(htmlBuilder);
		renderServices(htmlBuilder);
		renderFooter(htmlBuilder);

		return htmlBuilder.toString();
	}

	private void renderHeader(final StringBuilder htmlBuilder) {
		htmlBuilder.append("<html><head>\n");
		htmlBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"dashboard/css\"/>");
		htmlBuilder.append("<meta http-equiv=\"refresh\" content=\"");
		htmlBuilder.append(refreshPeriod);
		htmlBuilder.append("\"/>\n");
		htmlBuilder.append("</head>\n<body>\n");
		htmlBuilder.append("<table>\n<thead>\n<tr><td colspan=\"4\" class=\"faded\">");
		htmlBuilder.append(hostName);
		htmlBuilder
				.append("</td></tr>\n<tr><td class=\"faded\">Tjänstekontrakt - logisk adressat</th>");
		htmlBuilder.append("<td class=\"faded\">Genomsnittstid för ett anrop</th>");
		htmlBuilder.append("<td class=\"faded\">Antal anrop</th>");
		htmlBuilder.append("<td class=\"faded\">Antal misslyckade anrop</th>\n</thead>\n<tbody>\n");
	}

	private void renderServices(final StringBuilder htmlBuilder) {

		if (statistics == null) {
			return;
		}
		List<ServiceStatistics> values = new ArrayList<ServiceStatistics>(statistics.values());
		Collections.sort(values);
		for (ServiceStatistics serviceStat : values) {
			htmlBuilder.append("<tr><td class=\"state\">");
			htmlBuilder.append(serviceStat.producerId);
			htmlBuilder.append("</td><td class=\"state ");
			htmlBuilder.append(getStateForStatistics(serviceStat.averageDuration));
			htmlBuilder.append("\">");
			htmlBuilder.append(serviceStat.averageDuration == 0 ? "" : serviceStat.averageDuration);
			htmlBuilder.append("</td><td class=\"state\">");
			htmlBuilder.append(serviceStat.noOfCalls);
			htmlBuilder.append("</td><td class=\"state ");
			htmlBuilder
					.append(serviceStat.noOfCalls - serviceStat.noOfSuccesfullCalls > 0 ? "error"
							: "blank");
			htmlBuilder.append("\">");
			htmlBuilder.append(serviceStat.noOfCalls - serviceStat.noOfSuccesfullCalls);
			htmlBuilder.append("</td></tr>\n");
		}
	}

	private void renderFooter(final StringBuilder htmlBuilder) {
		htmlBuilder.append("<tr><td colspan=\"4\" class=\"faded\">");
		htmlBuilder.append(new Date().toString());
		htmlBuilder.append("</td></tr>\n</tbody>\n</table>\n</body></html>");
	}

	private String getStateForStatistics(long time) {
		return "blank";
//		if (time == 0) {
//			return "blank";
//		} else {
//			return "ok";
//		}
	}
}
