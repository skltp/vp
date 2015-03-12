/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
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
package se.skl.tp.vp.vagvalagent;

import java.util.Arrays;

import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.context.notification.MuleContextNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.hsa.cache.HsaCache;

public class MuleStartupNotificationHandler implements MuleContextNotificationListener<MuleContextNotification> {

	private static final Logger logger = LoggerFactory.getLogger(MuleStartupNotificationHandler.class);

	private VagvalAgent vagvalAgent;

	private HsaCache hsaCache;

	private String[] hsaFiles;

	public void setVagvalAgent(VagvalAgent vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	public void setHsaCache(HsaCache hsaCache) {
		this.hsaCache = hsaCache;
	}

	public void setHsaFiles(String... hsaFiles) {
		this.hsaFiles = hsaFiles;
	}

	@Override
	public void onNotification(MuleContextNotification notification) {
		if (notification.getType().equalsIgnoreCase(MuleContextNotification.TYPE_INFO)
				&& notification.getAction() == MuleContextNotification.CONTEXT_STARTED) {

			// Force a reset at startup
			logger.info("Initiates vagvalAgent");
			vagvalAgent.init(VagvalAgent.DONT_FORCE_RESET);

			logger.info("Initiates hsaCache with files: " + Arrays.toString(hsaFiles));
			hsaCache.init(hsaFiles);

			logger.info("Mule started, vagvalAgent and hsaCache successfully initiated");
		}
	}
}
