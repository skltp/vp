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

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.cxf.binding.soap.SoapFault;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.exceptions.VpTechnicalException;

public class ExceptionTransformer extends AbstractMessageAwareTransformer {

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ExceptionTransformer()  {
		registerSourceType(Object.class);
		setReturnClass(Object.class);
	}

	@Override
	public Object transform(MuleMessage msg, String encoding) throws TransformerException {
		// Check if any error
		if (msg.getExceptionPayload() != null) {
			logger.debug("Exceptionpayload detected!");
			String exceptionMessage = "";
			if (msg.getExceptionPayload().getException() instanceof RoutingException) {
				// Check what caused the routing exception
				RoutingException routingException = (RoutingException) msg.getExceptionPayload().getException();
				Throwable cause = routingException.getCause();
				if (cause != null) {
					// Check for defined TP exceptions
					if (cause instanceof VpSemanticException || cause instanceof VpTechnicalException) {
						return createSoapFault(msg, cause.getMessage(), null);
					} else {						
						Throwable nestedCause = cause;
						exceptionMessage = nestedCause.getMessage();
						// Just to be safe and avoid unlimited loops
						int i = 1;
						while (nestedCause.getCause() != null && i < 10){
							nestedCause = nestedCause.getCause();
							exceptionMessage = nestedCause.getMessage();
							
							// Try to see if we got a SoapFault response. If so we should recreate this and send back!
							if (nestedCause instanceof SoapFault) {
								logger.debug("SoapFault detected!");
								return createSoapFault(msg, "", (SoapFault)nestedCause);
							}
							i++;
						}						
					} 
				}
			}
			// No soapFault or TP exception found, returned found exception
			return createSoapFault(msg, "VP009 Exception when calling the service producer:" + exceptionMessage, null);
		} else if (msg.getPayload() instanceof org.mule.transport.NullPayload) {
			logger.debug("Nullpayload detected!");
			return createSoapFault(msg, "VP009 Exception when calling the service producer, connection closed", null);
		}
		
		// No error, return incoming payload!
		return msg.getPayload();
	}

	private Object createSoapFault(MuleMessage msg, String srcCause, SoapFault originalSoapFault) {
		StringBuffer result = new StringBuffer();
		
		// Check if to recreate a SoapFault or create a new
		if (originalSoapFault != null) {
			String faultCode = "soap:" + originalSoapFault.getFaultCode().getLocalPart();
			String faultString = originalSoapFault.getMessage();
			String faultDetail = getFaultDetail(originalSoapFault);
			reCreateSoapFault(result, faultCode, faultString, faultDetail);
		} else {
			createSoapFault(result, srcCause);
		}
				
		// Now wrap it into a soap-envelope
		result = createSoapEnvelope(result);

		// Clear out exception payload as we only return created SoapFaults when exception occurs
		msg.setExceptionPayload(null);
		
		// Tell Mule that we have returned a SoapFault
		logger.debug("SoapFault returned: " + result);
						
		// Set payload explict ??
		msg.setPayload(result.toString());
		
		// Done, return the string
		return result.toString();
	}

	private String getFaultDetail(SoapFault soapFault) {
		StringBuffer soapDetail = new StringBuffer();
		if (soapFault.hasDetails()) {
			logger.debug("getFaultDetail: Detail found.");
			Element detail = soapFault.getDetail();
			// Get xml string by applying fake XSLT transform of detail element
			try {
				// XSLT
				StringWriter xmlStringWriter = new StringWriter();
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document dom = db.newDocument();	
				Element root = dom.createElement("root");
	            dom.appendChild(root);  	            
	            Node detailNode = dom.importNode(detail,true); //true if you want a deep copy	  
	            Element root2 = dom.getDocumentElement();
	            root2.appendChild(detailNode);		
				DOMSource domSource = new DOMSource(dom);
				StreamResult streamResult = new StreamResult(xmlStringWriter);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer serializerXSLT = tf.newTransformer();
				serializerXSLT.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
				serializerXSLT.transform(domSource, streamResult);		
				// Strip off <root>  and </root> element from string
				soapDetail.append(xmlStringWriter.toString().substring(6, xmlStringWriter.toString().length()-7));
				logger.debug("XSLT transform:" + soapDetail);
			} catch(Exception ie) {
				logger.error("XSLT transform exception:" + ie.getMessage());
			}
			return soapDetail.toString();			
		} else {
			logger.debug("getFaultDetail: No detail found.");
			return "";
		}
	}
	
	private void reCreateSoapFault(StringBuffer result, String faultCode, String faultString, String faultDetail) {
		result.append("<soap:Fault>");
		result.append("<faultcode>" + faultCode + "</faultcode>");
		result.append("<faultstring>" + faultString + "</faultstring>");
		if (faultDetail != null && faultDetail.length() > 0) {
			result.append(faultDetail);
		}
		result.append("</soap:Fault>");
	}

	private void createSoapFault(StringBuffer result, String faultString) {
		result.append("<soap:Fault>");
		result.append("<faultcode>soap:Server</faultcode>");
		result.append("<faultstring>" + faultString + "</faultstring>");
		result.append("</soap:Fault>");
	}
	
	private StringBuffer createSoapEnvelope(StringBuffer result) {
		StringBuffer envelope = new StringBuffer(); 
		envelope.append("<?xml version='1.0' encoding='UTF-8'?>");
		envelope.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		envelope.append("<soap:Body>");
		envelope.append(result);
		envelope.append("</soap:Body>");
		envelope.append("</soap:Envelope>");
		return envelope;
	}
}
