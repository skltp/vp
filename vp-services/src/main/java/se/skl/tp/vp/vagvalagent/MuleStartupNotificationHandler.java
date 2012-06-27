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

import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.context.notification.MuleContextNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleStartupNotificationHandler implements MuleContextNotificationListener<MuleContextNotification> {

	private static final Logger logger = LoggerFactory
			.getLogger(MuleStartupNotificationHandler.class);

	private VagvalAgent vagvalAgent;

	public void setVagvalAgent(VagvalAgent vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	@Override
	public void onNotification(MuleContextNotification notification) {
		if (notification.getType().equalsIgnoreCase(MuleContextNotification.TYPE_INFO)
				&& notification.getAction() == MuleContextNotification.CONTEXT_STARTED) {
			logger.info("Mule started!!!, MuleStartupNotificationHandler initiates vagvalAgent");
			vagvalAgent.init();
		}
	}
}
