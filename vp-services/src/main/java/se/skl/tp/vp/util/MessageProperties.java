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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

public class MessageProperties implements MuleContextAware {

	private static final Logger logger = LoggerFactory.getLogger(MessageProperties.class);
	private static MuleContext context;
	private static MessageProperties _instance;
	private Properties propertyMap;
	
	/**
	 *  Called by spring using reflection and static method getInstance
	 */
	private MessageProperties() {
		try {
			propertyMap = new Properties();
			propertyMap.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("vp-messages.properties"));
		} catch (IOException e) {
			// Should not happen
			String msg = "VP012 Severe problem, VP does not have all necessary resources to operate. Error loading vp-messages";
			logger.error(msg);
			throw new VpSemanticException(msg, VpSemanticErrorCodeEnum.VP012);
		}
	}
	
	public void setPropertyMap(Map<String, String> propertyMap) {	
		this.propertyMap.putAll(propertyMap);
	}
	
	public String getValue(String key) {
		return (String)propertyMap.get(key);
	}

	@Override
	public void setMuleContext(MuleContext context) {
		MessageProperties.context = context;	
		_instance = this;
	}
	
	/**
	 * Basically called by non-spring classes
	 * @return
	 */
	public static MessageProperties getInstance() {
		if(_instance == null) {
	         synchronized(MessageProperties.class) {
				if(context == null)
					_instance = new MessageProperties();
				else
					_instance = context.getRegistry().lookupObject("messageProperties");
	         }
		}
		return _instance;
	}
	
	public String get(String key) {
		String msg = (String) getInstance().propertyMap.get(key);
		if(msg == null)
			msg = "An error occured. ";
		return msg;
	}
	
	public String get(String key, String suffix) {
		String msg = get(key);
		return msg.replace("{}", (suffix == null ? "" : suffix));
	}
	
	public String get(VpSemanticErrorCodeEnum errcode, String suffix) {
		String msg = get(errcode.getCode(), suffix);
		return errcode + " " + msg;
	}
	
}
