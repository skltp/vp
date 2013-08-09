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
package se.skl.tp.hsa.cache;

import java.util.List;

public interface HsaCache {
	
	/**
	 * Default root for the HSA cache is SE.
	 */
	public static final String DEFAUL_ROOTNODE = "SE";
	
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
	HsaCache init(String ... filenames) throws HsaCacheInitializationException;
	
	/**
	 * Get the parent HSA-ID for a specific HSA-ID. If the HSA-ID is not found in 
	 * HSA cache the default root parent is returned, in this case the SE node. 
	 * 
	 * @param hsaId the HSA-ID
	 * @return parent HSA-ID
	 * 
	 * @throws HsaCacheInitializationException if the cache has not been initialized
	 */
	String getParent(String hsaId) throws HsaCacheInitializationException;
	
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
