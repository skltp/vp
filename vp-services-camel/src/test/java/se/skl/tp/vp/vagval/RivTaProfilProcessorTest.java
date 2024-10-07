/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis). <http://cehis.se/>
 * <p>
 * This file is part of SKLTP.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.skl.tp.vp.vagval.RivTaProfilProcessor.RIV20;
import static se.skl.tp.vp.vagval.RivTaProfilProcessor.RIV21;
import static se.skl.tp.vp.vagval.RivTaProfilProcessor.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticException;

@CamelSpringBootTest
@SpringBootTest(classes = VagvalTestConfiguration.class)
public class RivTaProfilProcessorTest {

  @Autowired
  RivTaProfilProcessor rivTaProfilProcessor;

  @Test
  public void testVP001ThrownWhenNoRivProfile() throws Exception {
    
    Exception exception = assertThrows(
		  VpSemanticException.class, 
          () -> {

    Exchange exchange = createExchange(null, RIV20);
    addInBodyFromFile(exchange, "testfiles/GetSubjectOfCareRequest20.xml");
    rivTaProfilProcessor.process(exchange);
              }  );
    
    assertTrue(exception.getMessage().contains("VP001"));
  }

  @Test
  public void testVP005ThrownWhenNoOutProfileConversionIsPossible() throws Exception {

    Exception exception = assertThrows(
		  VpSemanticException.class, 
          () -> {
    Exchange exchange = createExchange(RIV20, "RIVTABP22");
    addInBodyFromFile(exchange, "testfiles/GetSubjectOfCareRequest20.xml");
    rivTaProfilProcessor.process(exchange);
          });
    
    assertTrue(exception.getMessage().contains("VP005"));
  }

  @Test
  public void testVP005ThrownWhenNoInProfileConversionIsPossible() throws Exception {

    Exception exception = assertThrows(
	  VpSemanticException.class, 
	  () -> {
	    Exchange exchange = createExchange("RIVTABP22", RIV20);
	    addInBodyFromFile(exchange, "testfiles/GetSubjectOfCareRequest20.xml");
	    rivTaProfilProcessor.process(exchange);
     });

    assertTrue(exception.getMessage().contains("VP005"));

  }

  @Test
  public void testRivVersionChangedWhenTransformationIsMade21To20() throws Exception {
    Exchange exchange = createExchange(RIV21, RIV20);
    addInBodyFromFile(exchange, "testfiles/GetSubjectOfCareRequest21.xml");
    rivTaProfilProcessor.process(exchange);

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/GetSubjectOfCareRequest20.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());
    ByteArrayOutputStream resultBody = exchange.getIn().getBody(ByteArrayOutputStream.class);

    XMLEventReader result = toXMLEventReader(resultBody);
    this.executeComparison(result, expected);
    assertEquals(RIV20, exchange.getProperty(VPExchangeProperties.RIV_VERSION));
  }

  @Test
  public void testNothingChangedWhenNoTransformation() throws Exception {
    Exchange exchange = createExchange(RIV21, RIV21);
    addInBodyFromFile(exchange, "testfiles/GetSubjectOfCareRequest21.xml");
    rivTaProfilProcessor.process(exchange);

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/GetSubjectOfCareRequest21.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());
    XMLStreamReader resultBody = (XMLStreamReader)exchange.getIn().getBody();
    this.executeComparison(toXMLEventReader(resultBody), expected);
    assertEquals(RIV21, exchange.getProperty(VPExchangeProperties.RIV_VERSION));
  }

  @Test
  public void testRiv21To20TransformerSpecial() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/GetSubjectOfCareRequest21.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/GetSubjectOfCareRequest20.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_ELEM,
        RivTaProfilProcessor.RIV20_ELEM);

    this.executeComparison(toXMLEventReader(data), expected);
  }

  @Test
  public void testRiv21To20Transformer() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/PingForConfiguration-request-rivtabp21-input.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/PingForConfiguration-expected-rivtabp20-result.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_ELEM,
        RivTaProfilProcessor.RIV20_ELEM);

    this.executeComparison(toXMLEventReader(data), expected);
  }

  @Test
  public void testRiv20To21Transformer() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/PingForConfiguration-request-input.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/PingForConfiguration-expected-result.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_ELEM,
        RivTaProfilProcessor.RIV21_ELEM);

    this.executeComparison(toXMLEventReader(data), expected);

  }

  @Test
  public void riv21To20WhenNamsespaceIsInAddressingElement() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader().getResource("testfiles/Rivta21Request.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/Rivta20Request.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_ELEM,
        RivTaProfilProcessor.RIV20_ELEM);

    this.executeComparison(toXMLEventReader(data), expected);
  }

  @Test
  public void riv20To21WhenNamsespaceIsInAddressingElement() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader().getResource("testfiles/Rivta20Request.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/Rivta21Request.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_ELEM,
        RivTaProfilProcessor.RIV21_ELEM);

    this.executeComparison(toXMLEventReader(data), expected);
  }

  @Test
  public void riv21To20WhenNamsespaceIsInHeaderElement() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/Rivta21RequestNamespaceInHeader.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/Rivta20RequestNamespaceInHeader.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_ELEM,
        RivTaProfilProcessor.RIV20_ELEM);

    System.out.println(new String(data.toByteArray(), "UTF-8"));

    this.executeComparison(toXMLEventReader(data), expected);
  }

  @Test
  public void riv20To21WhenNamsespaceIsInHeaderElement() throws Exception {
    final URL resource = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/Rivta20RequestNamespaceInHeader.xml");
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

    final URL resultFile = Thread.currentThread().getContextClassLoader()
        .getResource("testfiles/Rivta21RequestNamespaceInHeader.xml");
    final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

    final ByteArrayOutputStream data = RivTaProfilProcessor.transformXml(xstream,
        RivTaProfilProcessor.RIV20_NS, RivTaProfilProcessor.RIV21_NS, RivTaProfilProcessor.RIV20_ELEM,
        RivTaProfilProcessor.RIV21_ELEM);

    this.executeComparison(toXMLEventReader(data), expected);
  }

  private XMLEventReader toXMLEventReader(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
	    return XMLInputFactory.newInstance().createXMLEventReader(xmlStreamReader);
	  }

  private XMLEventReader toXMLEventReader(final ByteArrayOutputStream baos) throws XMLStreamException {
    return XMLInputFactory.newInstance().createXMLEventReader(new ByteArrayInputStream(baos.toByteArray()), UTF_8);
  }

  private void executeComparison(final XMLEventReader result, final XMLEventReader expected)
      throws Exception {

    System.out.println("Comparing xml results");
    while (expected.hasNext()) {
      final XMLEvent expectedEvent = nextEventIgnoreLF(expected);
      final XMLEvent resultEvent = nextEventIgnoreLF(result);

      if(expectedEvent.isStartDocument()){
        assertTrue(resultEvent.isStartDocument());
      }

      if (expectedEvent.isStartElement()) {

        final StartElement expectedElement = expectedEvent.asStartElement();
        final StartElement resultElement = resultEvent.asStartElement();

        System.out.println(expectedElement.getName().getLocalPart() + " == " + resultElement.getName().getLocalPart());

        assertEquals(expectedElement.getName().getLocalPart(), resultElement.getName().getLocalPart());
        assertEquals(expectedElement.getName().getPrefix(), resultElement.getName().getPrefix());
        assertEquals(expectedElement.getName().getNamespaceURI(), resultElement.getName().getNamespaceURI());

        @SuppressWarnings("rawtypes") final Iterator ns1 = expectedElement.getNamespaces();
        @SuppressWarnings("rawtypes") final Iterator ns2 = resultElement.getNamespaces();

        while (ns1.hasNext()) {
          final Namespace n1 = (Namespace) ns1.next();
          final Namespace n2 = (Namespace) ns2.next();

          System.out.println(n1.getName() + " == " + n2.getName());

          assertEquals(n1.getPrefix(), n2.getPrefix());
          assertEquals(n1.getValue(), n2.getValue());

        }
      }

      if (expectedEvent.isEndElement()) {

        final EndElement ee1 = expectedEvent.asEndElement();
        final EndElement ee2 = resultEvent.asEndElement();

        System.out.println(ee1.getName().getLocalPart() + " == " + ee2.getName().getLocalPart());

        assertEquals(ee1.getName().getLocalPart(), ee2.getName().getLocalPart());
        assertEquals(ee1.getName().getPrefix(), ee2.getName().getPrefix());
        assertEquals(ee1.getName().getNamespaceURI(), ee2.getName().getNamespaceURI());
      }
    }
  }

  private XMLEvent nextEventIgnoreLF(XMLEventReader expected) throws XMLStreamException {
    XMLEvent expectedEvent = expected.nextEvent();
    while(isNewLine(expectedEvent)){
      expectedEvent = expected.nextEvent();
    }
    return expectedEvent;
  }

  private boolean isNewLine(XMLEvent expectedEvent){
    if( expectedEvent.isCharacters()){
      String charData = expectedEvent.asCharacters().getData().trim();
      return charData.isEmpty();
    }
    return false;
  }

  private Exchange createExchange(String rivVersionIn, String rivVersionOut) throws IOException, XMLStreamException {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    ex.setProperty(VPExchangeProperties.RIV_VERSION, rivVersionIn);
    ex.setProperty(VPExchangeProperties.RIV_VERSION_OUT, rivVersionOut);
    return ex;
  }

  private void addInBodyFromFile(Exchange exchange, String fileName) throws IOException, XMLStreamException {
    final URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
    exchange.getIn().setBody(xstream);
  }
}
