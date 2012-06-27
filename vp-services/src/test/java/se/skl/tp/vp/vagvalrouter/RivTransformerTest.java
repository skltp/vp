package se.skl.tp.vp.vagvalrouter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.Before;
import org.junit.Test;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;

public class RivTransformerTest {

	private RivTransformer transformer;
	
	@Before
	public void setUp() throws Exception {
		this.transformer = new RivTransformer();
	}

	@Test
	public void testRiv21To20Transformer() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/PingForConfiguration-request-rivtabp21-input.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

		final URL resultFile = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/PingForConfiguration-expected-rivtabp20-result.xml");
		final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

		final ByteArrayOutputStream data = this.transformer.transformXml(new ReversibleXMLStreamReader(xstream),
				RivTransformer.RIV21_NS, RivTransformer.RIV20_NS, RivTransformer.RIV21_ELEM, RivTransformer.RIV20_ELEM);

		this.executeComparison(data, expected);
	}

	@Test
	public void testRiv20To21Transformer() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/PingForConfiguration-request-input.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

		final URL resultFile = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/PingForConfiguration-expected-result.xml");
		final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

		final ByteArrayOutputStream data = this.transformer.transformXml(new ReversibleXMLStreamReader(xstream),
				RivTransformer.RIV20_NS, RivTransformer.RIV21_NS, RivTransformer.RIV20_ELEM, RivTransformer.RIV21_ELEM);

		this.executeComparison(data, expected);

	}

	@Test
	public void riv21To20WhenNamsespaceIsInAddressingElement() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader().getResource("testfiles/Rivta21Request.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

		final URL resultFile = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20Request.xml");
		final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

		final ByteArrayOutputStream data = this.transformer.transformXml(new ReversibleXMLStreamReader(xstream),
				RivTransformer.RIV21_NS, RivTransformer.RIV20_NS, RivTransformer.RIV21_ELEM, RivTransformer.RIV20_ELEM);

		this.executeComparison(data, expected);
	}

	@Test
	public void riv20To21WhenNamsespaceIsInAddressingElement() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader().getResource("testfiles/Rivta20Request.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

		final URL resultFile = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta21Request.xml");
		final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

		final ByteArrayOutputStream data = this.transformer.transformXml(new ReversibleXMLStreamReader(xstream),
				RivTransformer.RIV20_NS, RivTransformer.RIV21_NS, RivTransformer.RIV20_ELEM, RivTransformer.RIV21_ELEM);

		this.executeComparison(data, expected);
	}
	
	@Test
	public void riv21To20WhenNamsespaceIsInHeaderElement() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader().getResource("testfiles/Rivta21RequestNamespaceInHeader.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

		final URL resultFile = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20RequestNamespaceInHeader.xml");
		final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

		final ByteArrayOutputStream data = this.transformer.transformXml(new ReversibleXMLStreamReader(xstream),
				RivTransformer.RIV21_NS, RivTransformer.RIV20_NS, RivTransformer.RIV21_ELEM, RivTransformer.RIV20_ELEM);

		this.executeComparison(data, expected);
	}

	@Test
	public void riv20To21WhenNamsespaceIsInHeaderElement() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader().getResource("testfiles/Rivta20RequestNamespaceInHeader.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());

		final URL resultFile = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta21RequestNamespaceInHeader.xml");
		final XMLEventReader expected = XMLInputFactory.newInstance().createXMLEventReader(resultFile.openStream());

		final ByteArrayOutputStream data = this.transformer.transformXml(new ReversibleXMLStreamReader(xstream),
				RivTransformer.RIV20_NS, RivTransformer.RIV21_NS, RivTransformer.RIV20_ELEM, RivTransformer.RIV21_ELEM);

		this.executeComparison(data, expected);
	}

	private void executeComparison(final ByteArrayOutputStream transformed, final XMLEventReader expected)
			throws Exception {

		final XMLEventReader result = XMLInputFactory.newInstance().createXMLEventReader(
				new ByteArrayInputStream(transformed.toByteArray()));

		System.out.println("Comparing xml results");
		while (expected.hasNext()) {
			final XMLEvent e1 = expected.nextEvent();
			final XMLEvent e2 = result.nextEvent();

			if (e1.isStartElement()) {

				final StartElement se1 = e1.asStartElement();
				final StartElement se2 = e2.asStartElement();

				System.out.println(se1.getName().getLocalPart() + " == " + se2.getName().getLocalPart());

				assertEquals(se1.getName().getLocalPart(), se2.getName().getLocalPart());
				assertEquals(se1.getName().getPrefix(), se2.getName().getPrefix());
				assertEquals(se1.getName().getNamespaceURI(), se2.getName().getNamespaceURI());

				@SuppressWarnings("rawtypes")
				final Iterator ns1 = se1.getNamespaces();
				@SuppressWarnings("rawtypes")
				final Iterator ns2 = se2.getNamespaces();

				while (ns1.hasNext()) {
					final Namespace n1 = (Namespace) ns1.next();
					final Namespace n2 = (Namespace) ns2.next();

					System.out.println(n1.getName() + " == " + n2.getName());

					assertEquals(n1.getPrefix(), n2.getPrefix());
					assertEquals(n1.getValue(), n2.getValue());

				}
			}

			if (e1.isEndElement()) {

				final EndElement ee1 = e1.asEndElement();
				final EndElement ee2 = e2.asEndElement();

				System.out.println(ee1.getName().getLocalPart() + " == " + ee2.getName().getLocalPart());

				assertEquals(ee1.getName().getLocalPart(), ee2.getName().getLocalPart());
				assertEquals(ee1.getName().getPrefix(), ee2.getName().getPrefix());
				assertEquals(ee1.getName().getNamespaceURI(), ee2.getName().getNamespaceURI());
			}
		}
	}
}
