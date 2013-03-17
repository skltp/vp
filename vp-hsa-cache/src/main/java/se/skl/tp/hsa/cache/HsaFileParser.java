/**
 * Copyright 2013 Sjukvardsradgivningen
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
package se.skl.tp.hsa.cache;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Parser for XML file with HsaObjects
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaFileParser {
 
	private static final String ELEMENT_NAME_ATTRIBUTE = "Attribute";
	private static final String ELEMENT_NAME_DN = "DN";
	private static final String ELEMENT_NAME_HSA_OBJECT = "HsaObject";
	private static final String ATTRIBUTE_NAME_HSA_IDENTITY = "hsaIdentity";
	private static final QName ATTRIBUTE_NAME_NAME = new QName("name");

	private static Logger log = LoggerFactory.getLogger(HsaFileParser.class);
	
	/**
	 * Parses XML file
	 * 
	 * @param filename file name
	 * 
	 * @return Map from {@link Dn} to {@link HsaNode}
	 * 
	 * @throws XMLStreamException thrown on XML parse exception.
	 * @throws IOException thrown if file cannot be read
	 */
	public Map<Dn, HsaNode> parse(String filename) throws XMLStreamException, IOException {
		return parse(new FileInputStream(filename));
	}
	
	/**
	 * Parse XML from inputstream
	 * 
	 * @param is inputstream
	 * 
	 * @return Map from {@link Dn} to {@link HsaNode}
	 * 
	 * @throws XMLStreamException thrown on XML parse exception.
	 * @throws IOException thrown if file cannot be read
	 */
	public Map<Dn, HsaNode> parse(InputStream is) throws XMLStreamException, IOException {
		return doParseFile(new BufferedInputStream(is));
	}
	
	/**
	 * Does the parsing
	 * 
	 * @param in inputstream
	 * 
	 * @return Map from {@link Dn} to {@link HsaNode}
	 * 
	 * @throws XMLStreamException thrown on XML parse exception.
	 * @throws IOException thrown if file cannot be read
	 */
	protected Map<Dn, HsaNode> doParseFile(InputStream in) throws XMLStreamException, IOException {
		Map<Dn, HsaNode> cache = new HashMap<Dn, HsaNode>();

		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
	
			HsaNode entry = null;
			long startRow = 0;
			
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				
				// When we hit a <HsaObject> tag
				if(event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					if (startElement.getName().getLocalPart() == ELEMENT_NAME_HSA_OBJECT) {
						startRow = startElement.getLocation().getLineNumber();
						entry = new HsaNode(startRow);
						continue;
					}				
				}
				// When we hit a </HsaObject> tag
				if(event.isEndElement()) {
					EndElement endElement = event.asEndElement();
					if (endElement.getName().getLocalPart() == ELEMENT_NAME_HSA_OBJECT) {
						if(entry.isValid()) {
							HsaNode previous = cache.put(entry.getDn(), entry);
							if(previous != null) {
								throw new IllegalStateException("HsaObject entry invalid @ LineNo:" + startRow + ", Duplicate with: " + previous.toString());
							}
						} else {
							logError("HsaObject entry invalid @ LineNo:" + startRow + ", entry: " + entry);
						}
						continue;
					}				
				}
				
				// When we hit a <DN> tag
				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					if (startElement.getName().getLocalPart() == ELEMENT_NAME_DN) {
						entry.setDn(eventReader.nextEvent().asCharacters().getData());
						continue;
					}
				}
				// When we hit a <Attribute> tag with attribute 'name'
				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();
					if (startElement.getName().getLocalPart() == ELEMENT_NAME_ATTRIBUTE) {
						Attribute attribute = startElement.getAttributeByName(ATTRIBUTE_NAME_NAME);
						if(attribute.getValue().equals(ATTRIBUTE_NAME_HSA_IDENTITY)) {
		                	eventReader.nextTag();	// Read the S tag
	                		entry.setHsaId(eventReader.nextEvent().asCharacters().getData());
	                		continue;
			            }
					}
				}
			}
		} finally {
			if(in != null) {
				in.close();
			}
		}
		return cache;
	}

	/**
	 * Log errors
	 * 
	 * @param msg
	 */
	protected void logError(String msg) {
		log.error(msg);
	}
	
}
