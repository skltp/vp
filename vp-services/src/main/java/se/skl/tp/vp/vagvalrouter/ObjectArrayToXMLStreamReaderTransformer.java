/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalrouter;

import java.io.OutputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.StringBufferOutputStream;

public class ObjectArrayToXMLStreamReaderTransformer extends AbstractTransformer {

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ObjectArrayToXMLStreamReaderTransformer() {
		registerSourceType(Object[].class);
		setReturnClass(XMLStreamReader.class);
	}

	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {

		if (logger.isDebugEnabled()) {
			logger.debug("src-type: {}", src.getClass().getName());
		}
		// Return the payload unchanged if a XMLStreamReader is not found in an
		// object array
		Object result = src;

		if (src instanceof Object[]) {
			XMLStreamReader reader = getXMLStreamReader(src);
			if (reader != null) {
				// We found a XMLStreamReader!
				if (logger.isDebugEnabled()) {
					logger.debug("Found a XMLStreamReader!");
				}
				result = reader;
			}
		}

		return result;
	}

	private XMLStreamReader getXMLStreamReader(Object payload) {
		XMLStreamReader reader = null;
		for (Object o : (Object[]) payload) {
			if (o instanceof XMLStreamReader) {
				reader = (XMLStreamReader) o;
				break;
			}
		}
		return reader;
	}

	private String XMLStreamReaderToString(XMLStreamReader reader) {

		try {
			StringBuffer sb = new StringBuffer();
			OutputStream os = new StringBufferOutputStream(sb);
			XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(os);

			XMLInputFactory xif = XMLInputFactory.newInstance();
			XMLEventReader r = xif.createXMLEventReader(reader);

			while (r.hasNext()) {
				XMLEvent event = r.nextEvent();
				writer.add(event);
			}

			writer.flush();

			return sb.toString();

		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		} catch (FactoryConfigurationError e) {
			throw new RuntimeException(e);
		}

	}
}
