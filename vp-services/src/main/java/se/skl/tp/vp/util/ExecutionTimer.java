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
package se.skl.tp.vp.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple utility to instrument different executions flows.
 *
 * @author Peter
 * @since 2.0
 */
public class ExecutionTimer {
	//
	private static final Logger logger = LoggerFactory.getLogger(ExecutionTimer.class);
	
	private static ThreadLocal<Map<String, ExecutionTimer>> local = new ThreadLocal<Map<String, ExecutionTimer>>();
	private String name;
	private long start;
	private long end;
	
	/**
	 * Inits timer functionality.
	 */
	public static void init() {
		logger.debug("init");
		local.set(new HashMap<String, ExecutionTimer>());
	}
	
	/**
	 * Removes all timers.
	 */
	public static void remove() {
		local.remove();
	}
	
	/**
	 * Returns the timer.
	 * @param name the name.
	 * @return the time by name, or null if not found.
	 */
	public static ExecutionTimer get(String name) {
		Map<String, ExecutionTimer> map = getMap();
		return (map == null) ? null : map.get(name);
	}
	
	/**
	 * Starts a timer with the given name.
	 * 
	 * @param name the name.
	 */
	public static void start(String name) {
		logger.debug(name);
		Map<String, ExecutionTimer> map = getMap();
		if (map != null) {
			map.put(name,  new ExecutionTimer(name));
		}
	}
	
	/**
	 * Stops timer with name.
	 * 
	 * @param name the name of the timer.
	 */
	public static void stop(String name) {
		logger.debug(name);
		ExecutionTimer timer = get(name);
		if (timer != null) {
			timer.stop();
			logger.debug(timer.toString());
		}
	}
		
	/**
	 * Returns a formatted string with timers.
	 */
	public static String format() {
		StringBuffer buf = new StringBuffer();
		buf.append("timers { ");
		int len = buf.length();
		Map<String, ExecutionTimer> map = getAll();
		if (map != null) {
			for (ExecutionTimer timer : map.values()) {
				if (buf.length() > len) {
					buf.append(", ");
				}
				buf.append(timer.toString());
			}
		}
		buf.append(" }");
		return buf.toString();
	}
	
	/**
	 * Returns all timers.
	 * 
	 * @return all timers, or null if none exists.
	 */
	public static Map<String, ExecutionTimer> getAll() {
		return getMap();
	}
	
	//
	private ExecutionTimer(String name) {
		this.name = name;
		this.start = now();
		this.end = -1L;
	}
	
	//
	private void stop() {
		if (this.end == -1L) {
			this.end = now();
		}
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns elapsed time in millis.
	 * 
	 * @return elapsed time in millis, or -1 if timer not has been stopped.
	 */
	public long getElapsed() {
		return (end == -1L) ? -1L : (end - start);
	}
	
	/**
	 * Returns a string representation.
	 * 
	 * @return the string as <name> { elapsed: <elapsed> }
	 */
	public String toString() {
		return String.format("%s: %d", getName(), getElapsed());
	}

	//
	private static long now() {
		return System.currentTimeMillis();
	}
	
	//
	private static Map<String, ExecutionTimer> getMap() {
		return local.get();
	}
}
