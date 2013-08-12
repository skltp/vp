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
package se.skl.tp.hsa.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link HsaCache}.
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaCacheImpl implements HsaCache {

	/**
	 * Logger
	 */
	private Logger log = LoggerFactory.getLogger(HsaCacheImpl.class);
	
	/**
	 * Map holding the cache
	 */
	private Map<String, HsaNode> cache;

	/**
	 * File parser
	 */
	private HsaFileParser parser = new HsaFileParser();
	
	/**
	 * Builds relations
	 */
	private HsaRelationBuilder builder = new HsaRelationBuilder();
	
	/**
	 * Default constructor initializes the cache in empty state  
	 */
	public HsaCacheImpl() {
		cache = new HashMap<String, HsaNode>();
	}
	
	/**
	 * Constructor that initializes the cache from a file.
	 * 
	 * @param filename
	 */
	public HsaCacheImpl(String ... filenames) {
		this.init(filenames);
	}
	
	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaCache#init(java.lang.String, java.lang.String)
	 */
	@Override
	public HsaCache init(String ... filenames) throws HsaCacheInitializationException {
		try {
			cache = doInitialize(filenames);
			log.info("HSA Cache initialized!");
		} catch (Exception e) {
			throw new HsaCacheInitializationException("Failed to initialize HSA cache!", e);
		}
		return this;
	}

	public HsaCache init(String filename, HsaRelationBuilder relationBuilder, HsaFileParser parser) {
		this.builder = relationBuilder;
		this.parser = parser;
		return init(filename);
	}
	/**
	 * Uses {@link HsaFileParser} and {@link HsaRelationBuilder} to populate cache.
	 * 
	 * @param filename path to file
	 * 
	 * @throws XMLStreamException thrown on XML parsing error.
	 * @throws IOException thrown if file cannot be read.
	 */
	private Map<String,HsaNode> doInitialize(String ... filenames) throws XMLStreamException, IOException {
		Map<Dn, HsaNode> hsaObjects = new HashMap<Dn,HsaNode>();
		for(String filename: filenames){
			hsaObjects.putAll(parser.parse(filename));
		}
		return builder.setRelations(hsaObjects);
	}

	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaCache#getParent(java.lang.String)
	 */
	@Override
	public String getParent(String hsaId) {
		try {
			HsaNode entry = getHsaNodeFromCache(hsaId);
			return (entry.getParent() != null) ? entry.getParent().getHsaId() : DEFAUL_ROOTNODE;
		} catch (HsaCacheNodeNotFoundException e) {
			return DEFAUL_ROOTNODE;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaCache#getChildren(java.lang.String)
	 */
	@Override
	public List<String> getChildren(String hsaId) throws HsaCacheNodeNotFoundException {
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
			logWarning("HsaNode with HSA-ID " + hsaId + " not found in cache!");
			throw new HsaCacheNodeNotFoundException("HsaNode with HSA-ID " + hsaId + " not found in cache!");
		}
		return entry;
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
	
	/**
	 * Log Warnings
	 * 
	 * @param msg message to log
	 */
	protected void logWarning(String msg) {
		log.warn(msg);		
	}
}
