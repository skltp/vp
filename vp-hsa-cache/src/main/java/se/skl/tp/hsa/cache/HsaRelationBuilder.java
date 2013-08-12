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

/**
 * Builds relations between parent and children.
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HsaRelationBuilder {
	
	private static Logger log = LoggerFactory.getLogger(HsaRelationBuilder.class);

	/**
	 * Processes a Map of {@link Dn} to {@link HsaNode} to a Map of String (HSA-ID) to {@link HsaNode}. All
	 * {@link HsaNode}s are updated with parent and children {@link HsaNode}s.
	 * 
	 * @param nodes read from XML file
	 * 
	 * @return Hsa Tree
	 */
	public Map<String, HsaNode> setRelations(final Map<Dn, HsaNode> nodes) {
		return transform(doSetRelations(nodes));
	}
	
	/**
	 * Updates relations on all nodes
	 * 
	 * @param nodes read from XML file
	 * 
	 * @return nodes with relations
	 */
	protected Map<Dn, HsaNode> doSetRelations(final Map<Dn, HsaNode> nodes) {
		for(Map.Entry<Dn, HsaNode> mapEntry : nodes.entrySet()) {
			HsaNode he = mapEntry.getValue();
			Dn parentDn = findParentDn(nodes, he.getDn());
			
			if(parentDn != null) {
				updateRelations(he, nodes.get(parentDn));				
			}
		}
		return nodes;
	}

	/**
	 * Sets relations for one node
	 * 
	 * @param node the node
	 * @param parent parent node
	 */
	private void updateRelations(HsaNode node, HsaNode parent) {
		node.setParent(parent);
		if(parent != null) {
			parent.addChild(node);
		}
	}

	/**
	 * Find parent {@link Dn} from {@link Dn}
	 * 
	 * @param nodes all nodes
	 * @param dn {@link Dn} to find parent {@link Dn} for.
	 * 
	 * @return parent {@link Dn} or null if none is found
	 */
	protected Dn findParentDn(final Map<Dn, HsaNode> nodes, Dn dn) {
		Dn parentDn = dn.parentDn();
		while(parentDn != null && nodes.get(parentDn) == null) {
			parentDn = parentDn.parentDn();
		}
		return parentDn;
	}
	
	/**
	 * Transforms Map from {@link Dn} to {@link HsaNode} to {@link String} to {@link HsaNode}
	 * 
	 * @param nodes the nodes
	 * 
	 * @return Map of {@link String} to {@link HsaNode}
	 */
	protected Map<String,HsaNode> transform(Map<Dn, HsaNode> nodes) {
		Map<String,HsaNode> result = new HashMap<String, HsaNode>();
		for(Map.Entry<Dn, HsaNode> mapEntry : nodes.entrySet()) {
			HsaNode entry = mapEntry.getValue();
			HsaNode parent = mapEntry.getValue().getParent();
			if(parent == null) {
				logWarning("No parent for HSA-ID="+entry.getHsaId()+", DN="+entry.getDn());
			}
			result.put(entry.getHsaId(), entry);
		}		
		return result;
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
