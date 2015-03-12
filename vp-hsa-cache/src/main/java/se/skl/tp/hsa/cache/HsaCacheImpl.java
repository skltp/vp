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
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
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
	 * @param filenames
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
	 * @param filenames path to file
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

    // public List<HsaNodeInfo> freeTextSearch(String searchText, int maxNoOFCharsNotMatching) {
    public List<HsaNodeInfo> freeTextSearch(String searchText, int maxNoOfHits) {

        // For future enhanced "close matching" search logic:
        // StringUtils.getLevenshteinDistance()

        log.debug("Looking for {} in the hsa-tree", searchText);

        List<HsaNodeInfo> list = new ArrayList<HsaNodeInfo>();
        String[] searchTexts = StringUtils.split((searchText == null) ? searchText : searchText.toUpperCase()); // Convert toUpper for performance reasons since StringUtil.containsIgnoreCase() perform an toUpper()

        // If the search text only contains whitespace or is null then return an empty list
        if (searchTexts == null || searchTexts.length == 0) return list;


        // Ok so we have a search text with at least one word, let's look for matches in the hsa cache
        Collection<HsaNode> nodes = cache.values();
        for (HsaNode node: nodes) {
            if (isMatch(searchTexts, node)) {
                list.add(new HsaInfoImpl(node));

                // Skip out if we reached maxNoOfHits...
                if (maxNoOfHits != -1 && list.size() >= maxNoOfHits) break;
            }
        }

        return list;
    }

    private boolean isMatch(String[] searchTexts, HsaNode node) {
        boolean match = true;
        for (String searchFragment: searchTexts) {

            // Fail if this searchFragment can't be found in neither the HSA-id nor in the DN-string
            if ((StringUtils.containsIgnoreCase(node.getHsaId(), searchFragment)) ||
                (StringUtils.containsIgnoreCase(node.getDn().toString(), searchFragment)) ||
                (StringUtils.containsIgnoreCase(node.getName(), searchFragment))) {

                log.debug("Found {} in {}", searchFragment, node.toString());

            } else {

                log.debug("Failed to find {} in {}, try next HSA-node", searchFragment, node.toString());
                match = false;
                break;
            }
        }
        return match;
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
