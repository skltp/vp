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

	private final int warningLevel;
	
	public HsaRelationBuilder(int warningLevel) {
		this.warningLevel = warningLevel;
	}
	
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
	private Dn findParentDn(final Map<Dn, HsaNode> nodes, Dn dn) {
		Dn parentDn = dn.parentDn();
		int levels = 1;
		while(parentDn != null && nodes.get(parentDn) == null) {
			levels++;
			parentDn = parentDn.parentDn();
		}
		if(warningLevel > 1 && levels > warningLevel) {
			logWarning("Parent on " + levels + " levels for [" + dn + "], parent is [" + parentDn + "]");
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
				logWarning("WARNING: No parent for HSA-ID="+entry.getHsaId()+", DN="+entry.getDn());
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
