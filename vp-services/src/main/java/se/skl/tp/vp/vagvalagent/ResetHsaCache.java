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

import java.util.Arrays;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheInitializationException;

public class ResetHsaCache implements Callable {

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
			hsaCache.init(hsaFiles);
			return "Reset HSA cache success using files: " + Arrays.toString(hsaFiles);
		} catch (HsaCacheInitializationException e) {
			return "Reset HSA cache failed using files: " + hsaFiles;
		}
	}
}
