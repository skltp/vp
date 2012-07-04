/* 
 * Licensed to the soi-toolkit project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The soi-toolkit project licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.skl.tp.vp.logmanager;

import javax.resource.spi.IllegalStateException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.util.ByteArrayInputStream;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.logentry.schema.v1.LogEvent;

/**
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 * 
 */
public class JmsToLogEventTransformer extends AbstractTransformer {

	private static final Logger log = LoggerFactory.getLogger(JmsToLogEventTransformer.class);

	private static final JAXBContext context = initContext();

	private static JAXBContext initContext() {
		try {
			return JAXBContext.newInstance(LogEvent.class);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to create JAXBContext for LogEvent", e);
		}
	}

	@Override
	protected Object doTransform(Object payload, String encoding) throws TransformerException {

		log.debug("Transforming {} to log event object", payload.getClass().getName());

		if (payload instanceof String) {

			final String msg = (String) payload;

			try {
				final LogEvent le = (LogEvent) context.createUnmarshaller().unmarshal(
						new ByteArrayInputStream(msg.getBytes()));

				return le;
			} catch (JAXBException e) {
				throw new TransformerException(this, e);
			}
		}

		throw new TransformerException(this, new IllegalStateException(
				String.format("Object to transform is not a String but was: %s", payload.getClass().getName())));
	}

}
