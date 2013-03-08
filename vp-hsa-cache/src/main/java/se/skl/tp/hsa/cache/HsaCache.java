/**
 * Copyright 2013 Sjukvardsradgivningen
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
package se.skl.tp.hsa.cache;

public interface HsaCache {
	
	/**
	 * Initialize the Cache
	 * 
	 * @param filename file to initialize from
	 * @param encoding encoding of the file, e.g. UTF-8
	 * @return a populated HsaCache
	 * 
	 * @throws HsaCacheInitializationException if a fatal error occurres initializing the file
	 */
	HsaCache init(String filename, String encoding) throws HsaCacheInitializationException;
	
	/**
	 * Get the parent HSA-ID for a specific HSA-ID
	 * 
	 * @param hsaId the HSA-ID
	 * @return parentHSA-ID
	 * 
	 * @throws HsaCacheNodeNotFoundException if the hsaId is not found in the cache
	 * @throws HsaCacheInitializationException if the cache has not been initialized
	 */
	String getParent(String hsaId) throws HsaCacheNodeNotFoundException, HsaCacheInitializationException;
}
