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
package se.skl.tp.vp.logging.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.core.lookup.StrLookup;

/**
 * Looks up keys from property file.
 * 
 * vp:socketappender.host = foo.bar.se
 * vp:socketappender.port = 12345
 * vp:socketappender.threshold = OFF | ALL
 */
@Plugin(name = "vp", category = StrLookup.CATEGORY)
public class PropertyFileLookup extends AbstractLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");
	private static final Properties props = new Properties();

	
    /**
     * Looks up the value for the key in the format "BundleName:BundleKey".
     *
     * For example: "com.domain.messages:MyKey".
     *
     * @param event
     *            The current LogEvent.
     * @param key
     *            the key to be looked up, may be null
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        
    	if (key == null) {
            return null;
        }
    	String value = getProperty(key);
        LOGGER.info(LOOKUP, "Retreived key/value {}={}", key, value);
        return value;

    }
    
    private static String getProperty(String key) {
    	
    	if(!"loaded".equals(props.getProperty("lookup.status"))) {
    		PropertyFileLookup.loadProperties();
    	}
    	return props.getProperty(key);
    }
    
	private static void loadProperties() {

		props.setProperty("lookup.status", "reload");

		String home = System.getProperty("mule.home");
	    Path path = FileSystems.getDefault().getPath(home, "conf", "vp-socketappender.properties");
		try {
			Files.getFileStore(path);
			try(BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				props.load(input);
			} 
			LOGGER.info(LOOKUP, "Property file loaded with {} keys.", props.keySet().size());
		} catch (IOException e) {
			props.setProperty("lookup.status", "failed");
            LOGGER.warn(LOOKUP, "Error loading property file vp-socketappender.properties", e);
			return;
		};
		
		props.setProperty("lookup.status", "loaded");
	}
}