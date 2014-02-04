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
package se.skl.tp.vp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class testing the EventLogger class
 *  
 * @author Magnus Ekstrand
 */
public class EventLoggerTest {

    /**
     * The class under test
     */
    private EventLogger eventLogger;

    /**
     * Initialization before each test
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        eventLogger = new EventLogger();
    }

    /**
     * A happy days test
     */
    @Test
    public void testPropertyValueFound() {
        assertEquals("SOITOOLKIT.LOG.STORE", eventLogger.getPropertyValue(EventLogger.PROPERTY_KEY_LOG_INFO_QUEUE));
        assertEquals("SOITOOLKIT.LOG.ERROR", eventLogger.getPropertyValue(EventLogger.PROPERTY_KEY_LOG_ERROR_QUEUE));
    }

    /**
     * A sad day test
     */
    @Test
    public void testPropertyValueNotFound() {
        assertTrue(null == eventLogger.getPropertyValue("THIS_PROP_SHALL_NOT_BE_FOUND"));
    }

    /**
     * A negative test expecting a RuntimeException to be thrown when
     * a property cannot be found or the value is an empty string. 
     */
    @Test(expected = RuntimeException.class)
    public void testQueueNameNotFound() {
        eventLogger.getQueueName("THIS_PROP_SHALL_NOT_BE_FOUND");
    }
}