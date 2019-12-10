/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis). <http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.requestreader;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Data;


public class PayloadInfoParser {

  public static final String RIVTABP_21 = "rivtabp21";
  public static final String RIVTABP_20 = "rivtabp20";

  // Static utility
  private PayloadInfoParser() {
  }

  public static PayloadInfo extractInfoFromPayload(final XMLStreamReader reader) throws XMLStreamException {
    PayloadInfo payloadInfo = new PayloadInfo();
    payloadInfo.setEncoding(reader.getEncoding());

    boolean headerFound = false;
    boolean bodyFound = false;

    while (reader.hasNext()) {
      if(reader.isStartElement()){
          String local = reader.getLocalName();

          if (bodyFound) {
            // We have found the element we need in the Header and Body, i.e. we
            // are done. Let's bail out!
            payloadInfo.setServiceContractNamespace(reader.getNamespaceURI());
            return payloadInfo;
          }

          //Body found, next element is the service interaction e.g GetSubjectOfCareSchedule
          if (local.equals("Body")) {
            bodyFound = true;
          }

          if (local.equals("Header")) {
            headerFound = true;
          }

          if (headerFound) {
            readHeader(reader, payloadInfo, local);
          }

      }
      reader.next();
    }

    return payloadInfo;
  }

  private static void readHeader(XMLStreamReader reader, PayloadInfo payloadInfo, String local) throws XMLStreamException {
    if (local.equals("To")) {
      payloadInfo.setRivVersion(RIVTABP_20);
      payloadInfo.setReceiverId(getReceiver(reader));
    } else if (local.equals("LogicalAddress")) {
      payloadInfo.setRivVersion(RIVTABP_21);
      payloadInfo.setReceiverId(getReceiver(reader));
    }
  }

  private static String getReceiver(XMLStreamReader reader) throws XMLStreamException {
    reader.next();
    if (!reader.isEndElement() && !reader.isWhiteSpace()) {
      return reader.getText();
    }
    return null;
  }

  @Data
  public static class PayloadInfo {
    String encoding;
    String receiverId;
    String rivVersion;
    String serviceContractNamespace;
  }

}
