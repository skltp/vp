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
import java.util.Map;

/**
 * Prints warnings to a {@link PrintWriter}. Set warning level to the number of missing parent nodes
 * you want a warning to be printed for.
 *
 * @author par.wenaker@callistaenterprise
 *
 */
public class HsaRelationBuilderWithLog extends HsaRelationBuilder {

	private final int warningLevel;
	private final PrintWriter w;
	
	/**
	 * Constructor
	 * 
	 * @param  pw {@link PrintWriter}
	 * @param warningLevel warning level used to print 
	 */
	public HsaRelationBuilderWithLog(PrintWriter pw, int warningLevel) {
		super();
		this.w = pw;
		this.warningLevel = warningLevel;
	}

	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaRelationBuilder#findParentDn(java.util.Map, se.skl.tp.hsa.cache.Dn)
	 */
	protected Dn findParentDn(final Map<Dn, HsaNode> nodes, Dn dn) {
		Dn parentDn = dn.parentDn();
		int levels = 1;
		while(parentDn != null && nodes.get(parentDn) == null) {
			levels++;
			parentDn = parentDn.parentDn();
		}
		if(warningLevel > 1 && levels > warningLevel && parentDn != null) {
			logWarning("Parent on " + levels + " levels for [" + dn + "], parent is [" + parentDn + "]");
		}
		return parentDn;
	}
	
	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaRelationBuilder#logWarning(java.lang.String)
	 */
	@Override
	protected void logWarning(String msg) {
		w.println("WARNING: " + msg);
	}
}
