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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Helper that prints a tree from a {@link HsaNode}
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaNodePrinter {
	
	/**
	 * The {@link HsaNode}
	 */
	private final HsaNode hsaNode;
	
	/**
	 * Number of spaces to use for indentation
	 */
	private final int indents;
	
	/**
	 * Cache for indentation Strings
	 */
	private final Map<Integer, String> cache = new HashMap<Integer,String>();
	
	/**
	 * Creates a printer instance
	 * 
	 * @param hsaNode the {@link HsaNode}
	 * @param indents number of spaces to use as indentation
	 */
	public HsaNodePrinter(final HsaNode hsaNode, final int indents) {
		this.hsaNode = hsaNode;
		this.indents = indents;
	}
	
	/**
	 * Prints the thee to the {@link PrintWriter}
	 * @param writer
	 */
	public void printTree(final PrintWriter writer) {
		doPrint(this.hsaNode, writer, 0);
	}
	
	/**
	 * Does the printing
	 * 
	 * @param hsaNode the {@link HsaNode}
	 * @param writer the {@link PrintWriter}
	 * @param indent number of indents to indent with
	 */
	protected void doPrint(HsaNode hsaNode, PrintWriter writer, int indent) {
		writer.println(getIndent(indent) + hsaNode.toString());
		for(HsaNode child : hsaNode.getChildren()) {
			doPrint(child, writer, indent + 1);
		}		
	}
	
	/**
	 * Get/Create indent string
	 * 
	 * @param indent number of indentations
	 * 
	 * @return string representing the number of indents
	 */
	private String getIndent(int indent) {
		String spaces = cache.get(indent);
		if(spaces == null) {
			spaces = StringUtils.repeat(" ", indent * indents);
			cache.put(indent, spaces);		
		}
		return spaces;
	}

}
