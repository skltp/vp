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
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.status.StatusLogger;


@Plugin(
        name = "SocketLoggerAppender",
        category = "Core",
        elementType = "appender",
        printObject = true
)
public class SocketLoggerAppender extends AbstractAppender {


    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");
    private static final Properties props = new Properties();

    private SocketAppender socketAppender;

    protected SocketLoggerAppender(String name, Filter filter, Layout<? extends Serializable> layout, SocketAppender socketAppender) {
        super(name, filter, layout);
        this.socketAppender = socketAppender;
    }

    @Override
    public void append(LogEvent logEvent) {
        if(socketAppender!=null){
            socketAppender.append(logEvent);
        }
    }

    @PluginFactory
    public static SocketLoggerAppender createAppender( @PluginAttribute("host") String host, @PluginAttribute("port") String port, @PluginAttribute("configResource") String configResource, @PluginAttribute("configFile") String configFile, @PluginAttribute("protocol") String protocolStr, @PluginElement("SSL") SslConfiguration sslConfig, @PluginAliases({"reconnectionDelay"}) @PluginAttribute("reconnectionDelayMillis") String delayMillis, @PluginAttribute("immediateFail") String immediateFail, @PluginAttribute("name") String name, @PluginAttribute("immediateFlush") String immediateFlush, @PluginAttribute("ignoreExceptions") String ignore, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filter") Filter filter, @PluginAttribute("advertise") String advertise, @PluginConfiguration Configuration config) {
        SocketAppender socketAppender = null;

        if(StringUtils.isNotEmpty(configResource)){
            loadPropertiesFromResource(configResource);
        }
        if(StringUtils.isNotEmpty(configFile)){
            loadPropertiesFromFile(configFile);
        }

        String hostname = StringUtils.isNotEmpty(host) ? host : getHostFromProps();
        String portnum = StringUtils.isNotEmpty(port) ? port : getPortFromProps();

        if( StringUtils.isNotEmpty(hostname) && StringUtils.isNotEmpty(portnum) && StringUtils.isNumericSpace(portnum)) {
            socketAppender = SocketAppender.createAppender(hostname, portnum, protocolStr, sslConfig, delayMillis, immediateFail, name, immediateFlush, ignore, layout, filter, advertise, config);
        } else{
            LOGGER.warn(LOOKUP, "No host/port defined, socketlogging will be turned off. Host:'{}' Port: {}", hostname, portnum );
        }
        return new SocketLoggerAppender(name, filter, layout, socketAppender);
    }

    private static String getPortFromProps() {
        return props.getProperty("socketappender.port");
    }

    private static String getHostFromProps() {
        return  props.getProperty("socketappender.host");
    }

    private static boolean loadPropertiesFromResource(String configResource) {
        try {
            InputStream inputStream = SocketLoggerAppender.class.getClassLoader().getResourceAsStream(configResource);
            props.load(inputStream);
            LOGGER.info(LOOKUP, "Property resource {} loaded with {} keys.", configResource, props.keySet().size());
        } catch (IOException e) {
            LOGGER.warn(LOOKUP, "Failed load property resource {}.", configResource );
            return false;
        }
        return true;
    }

    private static boolean loadPropertiesFromFile(String file) {
        Path path =  FileSystems.getDefault().getPath(file);
        try {
            Files.getFileStore(path);
            try(BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                props.load(input);
            }
            LOGGER.info(LOOKUP, "Property file loaded with {} keys.", props.keySet().size());
        } catch (IOException e) {
            LOGGER.warn(LOOKUP, "Failed load property file {}.", path.toString() );
            return false;
        }
        return true;
    }

}
