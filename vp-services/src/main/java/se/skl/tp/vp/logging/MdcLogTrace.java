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
package se.skl.tp.vp.logging;

import org.slf4j.MDC;

/**
 * Used to record important data that should be logged using the SLF4J MDC.
 * <p>
 * Note that MDC is a thread-bound context, will not work seamlessly
 * thread-switching is in play or asynch I/O.
 * </p>
 * 
 * @author hakan
 */
public class MdcLogTrace {
	/**
	 * The hsaId used for routing and a breadcrumb from the hsa-tree traversal
	 * (if the tree was traversed).
	 */
	public static final String ROUTER_RESOLVE_VAGVAL_TRACE = "routerVagvalTrace";
	/**
	 * The hsaId used for behorighet (authorization) and a breadcrumb from the
	 * hsa-tree traversal (if the tree was traversed).
	 */
	public static final String ROUTER_RESOLVE_ANROPSBEHORIGHET_TRACE = "routerBehorighetTrace";

	/**
	 * Clear the MDC.
	 */
	public static void clear() {
		MDC.clear();
	}

	public static void put(String key, String val) {
		MDC.put(key, val);
	}

	public static String get(String key) {
		return MDC.get(key);
	}
}
