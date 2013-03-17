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

import java.util.List;

public interface HsaCache {
	
	/**
	 * Initialize the Cache. If the cache has a value before a call to this method, that state is 
	 * retained in in case of an exception.
	 * 
	 * @param filename file to initialize from
	 * 
	 * @return a populated HsaCache or an unchanged HSA Cache in case of an exception
	 * 
	 * @throws HsaCacheInitializationException if a fatal error occurred initializing the file
	 */
	HsaCache init(String filename) throws HsaCacheInitializationException;
	
	/**
	 * Get the parent HSA-ID for a specific HSA-ID
	 * 
	 * @param hsaId the HSA-ID
	 * @return parent HSA-ID
	 * 
	 * @throws HsaCacheNodeNotFoundException if the hsaId is not found in the cache
	 * @throws HsaCacheInitializationException if the cache has not been initialized
	 */
	String getParent(String hsaId) throws HsaCacheNodeNotFoundException, HsaCacheInitializationException;
	
	/**
	 * Get the children HSA-ID for a specific HSA-ID
	 * 
	 * @param hsaId the HSA-ID
	 * 
	 * @return list of children HSA-ID. Can be empty.
	 * 
	 * @throws HsaCacheNodeNotFoundException if the hsaId is not found in the cache
	 * @throws HsaCacheInitializationException if the cache has not been initialized
	 */
	List<String> getChildren(String hsaId) throws HsaCacheNodeNotFoundException;
	
}
