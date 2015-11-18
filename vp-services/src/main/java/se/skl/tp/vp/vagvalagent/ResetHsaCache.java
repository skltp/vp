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

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheInitializationException;

public class ResetHsaCache implements Callable {

	private Logger log = LoggerFactory.getLogger(ResetHsaCache.class);
	private HsaCache hsaCache;
	private String[] hsaFiles;

	public void setHsaCache(HsaCache hsaCache) {
		this.hsaCache = hsaCache;
	}

	public void setHsaFiles(String... hsaFiles) {
		this.hsaFiles = hsaFiles;
	}

	public Object onCall(final MuleEventContext eventContext) throws Exception {

		final String content = resetCache(eventContext);
		eventContext.setStopFurtherProcessing(true);
		return content;
	}

	private String resetCache(final MuleEventContext eventContext) {
		try {
			log.info("Start a reset of HSA cache using files: {} ...", Arrays.toString(hsaFiles));
			HsaCache cache = hsaCache.init(hsaFiles);
			int cacheSize = cache.getHSACacheSize();
			if (cacheSize > 0) {
				log.info("Succesfully reset HSA cache. HSA cache now contains: " + cacheSize + " entries");
				return "Succesfully reset HSA cache using files: " + Arrays.toString(hsaFiles) + 
						"\nHSA cache now contains: " + cacheSize + " entries";
			} else {
				log.warn("HSA cache reset to 0 entries, HSA cache is now empty!");
				return "Warning: HSA cache reset to 0 entries using files: " + 
						Arrays.toString(hsaFiles) + " HSA cache is now empty!";
			}
		} catch (HsaCacheInitializationException e) {
			log.error("Reset HSA cache failed", e);
			return "Reset HSA cache failed using files: " + Arrays.toString(hsaFiles);
		}
	}
}
