/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis). <http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.io.UnsupportedEncodingException;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;
import se.skltp.takcache.TakCacheLog;

public class ResetVagvalCache implements Callable {

  private VagvalAgentInterface vagvalAgent;

  public void setVagvalAgent(VagvalAgentInterface vagvalAgent) {
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
    eventContext.getMessage()
        .setProperty("Content-Type", "text/html; charset=" + eventContext.getEncoding(),
            PropertyScope.INBOUND);

    // Reset cache
    TakCacheLog takCacheLog = vagvalAgent.resetVagvalCache();
    return getResultAsString(takCacheLog);
  }

  private void setResponseContentLength(final MuleEventContext eventContext, final String content)
      throws UnsupportedEncodingException {

    eventContext.getMessage().setProperty("Content-Length",
        Integer.toString(content.getBytes(eventContext.getEncoding()).length),
        PropertyScope.INBOUND);
  }

  private String getResultAsString(TakCacheLog takCacheLog) {

    StringBuilder resultAsString = new StringBuilder();
    for (String processingLog : takCacheLog.getLog()) {
      resultAsString.append("<br>").append(processingLog);
    }

    return resultAsString.toString();
  }

}
