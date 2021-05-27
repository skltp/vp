package se.skl.tp.vp.vagval;

import java.io.ByteArrayOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpTechnicalException;

@Component
public class RivTaProfilProcessor implements Processor {

    public static final String UTF_8 = "UTF-8";
    private static Logger log = LoggerFactory.getLogger(RivTaProfilProcessor.class);

    static final String RIV20 = "RIVTABP20";
    static final String RIV21 = "RIVTABP21";

    static final String RIV20_NS = "http://www.w3.org/2005/08/addressing";
    static final String RIV20_ELEM = "To";

    static final String RIV21_NS = "urn:riv:itintegration:registry:1";
    static final String RIV21_ELEM = "LogicalAddress";

    private static XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    @Autowired
    ExceptionUtil exceptionUtil;

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Riv transformer executing");

        String rivVersionIn = (String) exchange.getProperty(VPExchangeProperties.RIV_VERSION);
        String rivVersionOut = (String) exchange.getProperty(VPExchangeProperties.RIV_VERSION_OUT);
        System.out.println(">>>>>>>>>>>>>>>>" + rivVersionIn + " " + rivVersionOut);
        if(rivVersionIn == null){
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP001);
        }

        if (!rivVersionIn.equalsIgnoreCase(rivVersionOut)) {
            if (rivVersionIn.equalsIgnoreCase(RIV20) && rivVersionOut.equalsIgnoreCase(RIV21)) {
            	ByteArrayOutputStream strPayload = doTransform(exchange, RIV20_NS, RIV21_NS, RIV20_ELEM, RIV21_ELEM);
                exchange.getIn().setBody(strPayload);
            } else if (rivVersionIn.equalsIgnoreCase(RIV21) && rivVersionOut.equalsIgnoreCase(RIV20)) {
            	ByteArrayOutputStream strPayload = doTransform(exchange, RIV21_NS, RIV20_NS, RIV21_ELEM, RIV20_ELEM);
                exchange.getIn().setBody(strPayload);
            }else {
                throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP005, rivVersionIn);
            }
            exchange.setProperty(VPExchangeProperties.RIV_VERSION, rivVersionOut);
        }
    }

    static ByteArrayOutputStream doTransform(final Exchange msg, final String fromNs, final String toNs, final String fromElem,
                                             final String toElem) {
        log.info("Transforming {} -> {}. ", fromNs, toNs);
        
        try {
            return transformXml(msg.getIn().getBody(XMLStreamReader.class), fromNs, toNs, fromElem, toElem);
        } catch (Exception e) {
            log.error("RIV transformation failed", e);
            throw new VpTechnicalException(e);
        }
    }

    static ByteArrayOutputStream transformXml(XMLStreamReader reader,
                                              final String fromAddressingNs,
                                              final String toAddressingNs, final String fromAddressingElement,
                                              final String toAddressingElement) throws XMLStreamException {

        ByteArrayOutputStream os = new ByteArrayOutputStream(2048);
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(os, UTF_8);

        writer.writeStartDocument();

        int read = 0;
        int event = reader.getEventType();

        while (reader.hasNext()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    read++;
                    writeStartElement(reader, writer, fromAddressingNs, toAddressingNs, fromAddressingElement, toAddressingElement);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    read--;
                    if (read <= 0) {
                        writer.writeEndDocument();
                        return os;
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(reader.getText());
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                case XMLStreamConstants.END_DOCUMENT:
                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.NAMESPACE:
                    break;
                case XMLStreamConstants.COMMENT:
                    writer.writeComment(reader.getText());
                    break;
                default:
                    break;
            }
            event = reader.next();
        }
        writer.writeEndDocument();

        return os;
    }


    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer,
                                          final String fromAddressingNs,
                                          final String toAddressingNs,
                                          final String fromAddressingElement,
                                          final String toAddressingElement)
            throws XMLStreamException {

        String uri = reader.getNamespaceURI();
        if (fromAddressingNs.equals(uri)) {
            if (log.isDebugEnabled()) {
                log.debug("RivTransformer { fromNS: {}, toNS: {} }", new Object[]{fromAddressingNs, toAddressingNs});
            }
            uri = toAddressingNs;
        }

        String local = reader.getLocalName();
        // make sure we only transforms element names within the right namespace
        if (fromAddressingElement.equals(local) && toAddressingNs.equals(uri)) {
            local = toAddressingElement;
            if (log.isDebugEnabled()) {
                log.debug("RivTransformer { fromName: {}, toName: {}, uri: {} }", fromAddressingElement, toAddressingElement, uri);
            }
        }

        String prefix = reader.getPrefix();
        if (prefix == null) {
            prefix = "";
        }

        boolean writeElementNS = false;
        if (uri != null) {
            String boundPrefix = writer.getPrefix(uri);
            if (boundPrefix == null || !prefix.equals(boundPrefix)) {
                writeElementNS = true;
            }
        }

        // Write out the element name
        writeOutElementName(writer, uri, local, prefix);

        // Write out the namespaces
        writeElementNS = writeOutNameSpaces(reader, writer, fromAddressingNs, toAddressingNs,
            toAddressingElement, uri,
            local, prefix, writeElementNS);

        // Check if the namespace still needs to be written.
        // We need this check because namespace writing works
        // different on Woodstox and the RI.
        if (writeElementNS) {
            if (prefix.length() == 0) {
                writer.writeDefaultNamespace(uri);
            } else {
                writer.writeNamespace(prefix, uri);
            }
        }

        // Write out attributes
        writeOutAttributes(reader, writer, fromAddressingNs, toAddressingNs, toAddressingElement,
            local);
    }

    private static void writeOutElementName(XMLStreamWriter writer, String uri, String local,
        String prefix) throws XMLStreamException {
        if (uri != null) {
            if (prefix.length() == 0 && StringUtils.isEmpty(uri)) {
                writer.writeStartElement(local);
                writer.setDefaultNamespace(uri);

            } else {
                writer.writeStartElement(prefix, local, uri);
                writer.setPrefix(prefix, uri);
            }
        } else {
            writer.writeStartElement(local);
        }
    }

    private static boolean writeOutNameSpaces(XMLStreamReader reader, XMLStreamWriter writer,
        String fromAddressingNs, String toAddressingNs, String toAddressingElement, String uri,
        String local, String prefix, boolean writeElementNS) throws XMLStreamException {
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsURI = reader.getNamespaceURI(i);
            if (fromAddressingNs.equals(nsURI) && ("Envelope".equals(local) || "Header".equals(local)
                    || toAddressingElement.equals(local))) {
                nsURI = toAddressingNs;
            }

            String nsPrefix = reader.getNamespacePrefix(i);
            if (nsPrefix == null) {
                nsPrefix = "";
            }

            if (nsPrefix.length() == 0) {
                writer.writeDefaultNamespace(nsURI);
            } else {
                writer.writeNamespace(nsPrefix, nsURI);
            }

            if (nsURI.equals(uri) && nsPrefix.equals(prefix)) {
                writeElementNS = false;
            }
        }
        return writeElementNS;
    }

    private static void writeOutAttributes(XMLStreamReader reader, XMLStreamWriter writer,
        String fromAddressingNs, String toAddressingNs, String toAddressingElement, String local)
        throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String ns = reader.getAttributeNamespace(i);

            if (fromAddressingNs.equals(ns) && toAddressingElement.equals(local)) {
                ns = toAddressingNs;
            }

            String nsPrefix = reader.getAttributePrefix(i);
            if (ns == null || ns.length() == 0) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else if (nsPrefix == null || nsPrefix.length() == 0) {
                writer.writeAttribute(reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(reader.getAttributePrefix(i), reader.getAttributeNamespace(i), reader
                        .getAttributeLocalName(i), reader.getAttributeValue(i));
            }

        }
    }
}
