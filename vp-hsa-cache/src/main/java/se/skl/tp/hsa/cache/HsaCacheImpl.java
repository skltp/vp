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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

/**
 * Implementation of {@link HsaCache}.
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaCacheImpl implements HsaCache {

	/**
	 * Map holding the cache
	 */
	Map<String, HsaNode> cache;

	/**
	 * Builds relations
	 */
	HsaRelationBuilder builder = new HsaRelationBuilder(-1);
	
	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaCache#init(java.lang.String, java.lang.String)
	 */
	@Override
	public HsaCache init(String filename, String encoding) throws HsaCacheInitializationException {
		try {
			doInitialize(filename, encoding);
		} catch (Exception e) {
			throw new HsaCacheInitializationException("Failed to initialize cache!!", e);
		}
		return this;
	}

	public HsaCache init(String filename, String encoding, int warning) {
		builder = new HsaRelationBuilder(warning);
		return init(filename, encoding);
	}
	/**
	 * Uses {@link HsaFileParser} and {@link HsaRelationBuilder} to populate cache.
	 * 
	 * @param filename path to file
	 * @param encoding encoding of file
	 * 
	 * @throws XMLStreamException thrown on XML parsing error.
	 * @throws IOException thrown if file cannot be read.
	 */
	private void doInitialize(String filename, String encoding)
			throws XMLStreamException, IOException {
		HsaFileParser parser = new HsaFileParser();
		Map<Dn, HsaNode> hsaObjects = parser.parse(filename, encoding);

		cache = builder.setRelations(hsaObjects);
	}

	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaCache#getParent(java.lang.String)
	 */
	@Override
	public String getParent(String hsaId) throws HsaCacheNodeNotFoundException {
		checkInitialized();		
		HsaNode entry = getHsaNodeFromCache(hsaId);

		return (entry.getParent() != null) ? entry.getParent().getHsaId()
				: null;
	}

	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaCache#getChildren(java.lang.String)
	 */
	@Override
	public List<String> getChildren(String hsaId) throws HsaCacheNodeNotFoundException {
		checkInitialized();
		HsaNode entry = getHsaNodeFromCache(hsaId);
		
		List<String> childHsaIds = new ArrayList<String>();
		for(HsaNode child : entry.getChildren()){
			childHsaIds.add(child.getHsaId());
		}
		return childHsaIds;
	}
	
	private HsaNode getHsaNodeFromCache(String hsaId) throws HsaCacheNodeNotFoundException {
		HsaNode entry = cache.get(hsaId);
		if (entry == null) {
			throw new HsaCacheNodeNotFoundException("HsaNode with HSA-ID " + hsaId + " not found in cache!");
		}
		return entry;
	}

	private void checkInitialized() throws HsaCacheInitializationException {
		if(cache == null) {
			throw new HsaCacheInitializationException("HsaCache is NOT initialized yet!");
		}
	}

	
	/**
	 * Gets a node with the specified HSA-ID
	 * 
	 * @param hsaId HSA-ID
	 * 
	 * @return {@link HsaNode} object
	 */
	public HsaNode getNode(String hsaId) {
		return cache.get(hsaId);
	}


}
