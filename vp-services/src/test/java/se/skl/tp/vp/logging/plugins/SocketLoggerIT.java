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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.server.ObjectInputStreamLogEventBridge;
import org.apache.logging.log4j.core.net.server.TcpSocketServer;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketLoggerIT {
    public static final String LOG_MESSAGE = "Message to send over socket appender";
    private static LogEvent logEvent;


    @BeforeClass
    public static void beforeClass() throws IOException {
        startSocketServer();
    }

    @AfterClass
    public static void afterClass(){
        System.setProperty("SocketLoggerAppenderTest.port", "");
    }

    @Test
    public void logEventTest() throws IOException {

        Logger socketLogger = LoggerFactory.getLogger("se.skltp.mule.logging.socketLogger");
        socketLogger.error(LOG_MESSAGE);

        waitTime(1000);
        Assert.assertNotNull("No message received on socket as expected!", logEvent);
        Assert.assertEquals(LOG_MESSAGE, logEvent.getMessage().getFormattedMessage());

    }

    private static void startSocketServer() throws IOException {
        int port = AvailablePortFinder.getNextAvailable();
        System.setProperty("SocketLoggerAppenderTest.port", String.valueOf(port));
        LoghandlerTcpSocketServer tcpSocketServer = new LoghandlerTcpSocketServer(port);
        final Thread tcpThread = new Thread(tcpSocketServer);
        tcpThread.start();
    }

    private void waitTime(int time) {
        synchronized (this) {
            try {
                wait(time);
            } catch (InterruptedException e) {
            }
        }
    }

    static class LoghandlerTcpSocketServer extends TcpSocketServer {

        public LoghandlerTcpSocketServer(int port) throws IOException {
            super(port, new ObjectInputStreamLogEventBridge(){
                @Override
                public ObjectInputStream wrapStream(final InputStream inputStream) throws IOException {
                    return new ObjectInputStream(inputStream);
                }
            });
        }

        @Override
        public void log(final LogEvent event) {
            logEvent = event;
        }
    }
}


