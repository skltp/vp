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

public class HttpHeaders {

	/**
	 * HTTP Header holding producer response time, forwarded to consumer.
	 * <p>
	 *
	 * @since VP-2.2.1
	 */
	public static final String X_SKLTP_PRODUCER_RESPONSETIME = "x-skltp-prt";
	/**
	 * HTTP Header holding correlation id forwarded to producer.
	 * <p>
	 *
	 * @since VP-2.2.12
	 */
	public static final String X_SKLTP_CORRELATION_ID = "x-skltp-correlation-id";
	/**
	 * HTTP Header forwarded to producer. Note that header represent original consumer and should not be used for routing or authorization
	 * in SKLTP VP. For routing and authorization use X_VP_SENDER_ID.
	 * <p>
	 *
	 * @since VP-2.0
	 */
	public static final String X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID = "x-rivta-original-serviceconsumer-hsaid";
	/**
	 * HTTP Header x-vp-sender-id, identifies the consumer doing the actual call to SKLTP VP. The header x-vp-sender-id
	 * should always exist in outbound calls for other SKLTP component that uses it.
	 *
	 * @since VP-2.2.3
	 */
	public static final String X_VP_SENDER_ID = "x-vp-sender-id";
	/**
	 * HTTP header x-vp-instance-id, carrying information regarding the VP instance id, either incoming requests
	 * or outgoing. This header can be used by other VP instances to make sure VP internal http headers are not
	 * processed.
	 *
	 * @since VP-2.2.4
	 */
	public static final String X_VP_INSTANCE_ID = "x-vp-instance-id";
	/**
	 * Incoming HTTP Header x-vp-auth-cert, carrying a X509 certificate, used when implementing a reverse proxy.
	 *
	 * @since VP-1.3
	 */
	public static final String REVERSE_PROXY_HEADER_NAME = "x-vp-auth-cert";
	
	/**
	 * User-Agent for outgoing requests from VP
	 * TODO: Make this configurable rather than a static variable.
	 */
	public static final String VP_HEADER_USER_AGENT = "SKLTP VP/3.1";

}
