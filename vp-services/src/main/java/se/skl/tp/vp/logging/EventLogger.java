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

import java.util.Map;

import org.soitoolkit.commons.mule.jaxb.JaxbObjectToXmlTransformer;

public interface EventLogger <T> {

	/**
	 * Enable logging to JMS, it true by default
	 * 
	 * @param logEnableToJms
	 */
	void setEnableLogToJms(boolean enableLogToJms);

	/**
	 * Specify to which queue error messages should be sent.
	 * 
	 * @param logErrorQueueName
	 */
	void setLogErrorQueueName(String logErrorQueueName);

	/**
	 * Specify to which queue info messages should be sent.
	 * 
	 * @param logInfoQueueName
	 */
	void setLogInfoQueueName(String logInfoQueueName);

	/**
	 * For socket logging
	 * @param useSocketLogger
	 */
	public void setUseSocketLogger(Boolean useSocketLogger);
	public void setSocketLoggerCategories(String categories);

	/**
	 * Set context
	 * @param context
	 */
	<F> void setContext(F context);

	/**
	 * Setter for the jaxbToXml property
	 * 
	 * @param jaxbToXml
	 */
	void setJaxbToXml(JaxbObjectToXmlTransformer jaxbToXml);

	void logInfoEvent(T message, String logMessage, Map<String, String> businessContextId,
			SessionInfo extraInfo);

	void logErrorEvent(Throwable error, T message, Map<String, String> businessContextId,
			SessionInfo extraInfo);

	void logErrorEvent(Throwable error, String payload, Map<String, String> businessContextId,
			SessionInfo extraInfo);

}