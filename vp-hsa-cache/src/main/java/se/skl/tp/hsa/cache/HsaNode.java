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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Represents a Hsa Node in the Tree. Has one parent and children.
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaNode {
	
	/**
	 * Distinguished Name of this node
	 */
	private Dn dn;
	
	/**
	 * Line in XML file that this node was read from (for debug)
	 */
	private long lineNo;
	
	/**
	 * HSA-ID of this node
	 */
	private String hsaId;
	
	/**
	 * Parent for this node
	 */
	private HsaNode parent;
	
	/**
	 * Children of this node
	 */
	private List<HsaNode> children = new ArrayList<HsaNode>();
	
	/**
	 * Creates a new node
	 * 
	 * @param lineNo line where this node was read
	 */
	public HsaNode(long lineNo) {
		this.lineNo = lineNo;
	}
	
	public void setDn(String dnStr) {
		this.dn = new Dn(dnStr);
	}
	
	public Dn getDn() {
		return dn;
	}
	
	public void setHsaId(String hsaId) {
		this.hsaId = hsaId;
	}
	
	public String getHsaId() {
		return hsaId;
	}
	
	public void setParent(HsaNode parent) {
		this.parent = parent;
	}
	
	public HsaNode getParent() {
		return parent;
	}
	
	public void addChild(HsaNode hsaObject) {
		children.add(hsaObject);
	}
	
	public List<HsaNode> getChildren() {
		return new ArrayList<HsaNode>(children);
	}
		
	@Override
	public String toString() {
		return "dn="+dn+",hsaId="+hsaId+",lineNo="+lineNo;
	}

	public boolean isValid() {
		return StringUtils.isNotBlank(hsaId) && dn != null;
	}
}
