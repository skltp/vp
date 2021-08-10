package se.skl.tp.vp.requestreader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import se.skl.tp.vp.exceptions.VpTechnicalException;

public class RequestReaderProcessorXMLEventReaderTest {

  public static final String EXPECTED_RESULT_1 = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Header><h:LogicalAddress xmlns:h=\"urn:riv:itintegration:registry:1\" xmlns=\"urn:riv:itintegration:registry:1\">HttpProducer</h:LogicalAddress></s:Header><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><CheckBlocksRequest xmlns=\"urn:riv:ehr:blocking:accesscontrol:CheckBlocksResponder:3\"><AccessingActor><EmployeeId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">TST5565594230-114W</EmployeeId><CareProviderId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748100S</CareProviderId><CareUnitId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE55643237481011</CareUnitId></AccessingActor><PatientId>191212121212</PatientId><InformationEntities><InformationStartDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-14T10:40:00</InformationStartDate><InformationEndDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-14T10:40:00</InformationEndDate><InformationCareUnitId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748101M</InformationCareUnitId><InformationCareProviderId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748100S</InformationCareProviderId><InformationType xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">lak</InformationType><RowNumber xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">0</RowNumber></InformationEntities><InformationEntities><InformationStartDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-15T09:01:00</InformationStartDate><InformationEndDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-15T09:01:00</InformationEndDate><InformationCareUnitId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748101M</InformationCareUnitId><InformationCareProviderId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748100S</InformationCareProviderId><InformationType xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">lak</InformationType><RowNumber xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">1</RowNumber></InformationEntities></CheckBlocksRequest></s:Body></s:Envelope>";
  public static final String MTOM_TEXT_1 = "\n"
      + "--uuid:2b0d9d3d-f8bf-4a78-b9ad-62e0996d1872+id=1\n"
      + "Content-ID: <http://tempuri.org/0>\n"
      + "Content-Transfer-Encoding: 8bit\n"
      + "Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"\n"
      + "\n"
      + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Header><h:LogicalAddress xmlns:h=\"urn:riv:itintegration:registry:1\" xmlns=\"urn:riv:itintegration:registry:1\">HttpProducer</h:LogicalAddress></s:Header><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><CheckBlocksRequest xmlns=\"urn:riv:ehr:blocking:accesscontrol:CheckBlocksResponder:3\"><AccessingActor><EmployeeId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">TST5565594230-114W</EmployeeId><CareProviderId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748100S</CareProviderId><CareUnitId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE55643237481011</CareUnitId></AccessingActor><PatientId>191212121212</PatientId><InformationEntities><InformationStartDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-14T10:40:00</InformationStartDate><InformationEndDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-14T10:40:00</InformationEndDate><InformationCareUnitId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748101M</InformationCareUnitId><InformationCareProviderId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748100S</InformationCareProviderId><InformationType xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">lak</InformationType><RowNumber xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">0</RowNumber></InformationEntities><InformationEntities><InformationStartDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-15T09:01:00</InformationStartDate><InformationEndDate xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">2017-03-15T09:01:00</InformationEndDate><InformationCareUnitId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748101M</InformationCareUnitId><InformationCareProviderId xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">SE5564323748100S</InformationCareProviderId><InformationType xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">lak</InformationType><RowNumber xmlns=\"urn:riv:ehr:blocking:accesscontrol:3\">1</RowNumber></InformationEntities></CheckBlocksRequest></s:Body></s:Envelope>\n"
      + "--uuid:2b0d9d3d-f8bf-4a78-b9ad-62e0996d1872+id=1--";

  public static final String MTOM_TEXT_2 = "\n"
      + "------=_Part_11_8757200.1571821395690\n"
      + "Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\"\n"
      + "Content-Transfer-Encoding: 8bit\n"
      + "Content-ID: <rootpart@soapui.org>\n"
      + "\n"
      + "<soapenv:envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:riv:infrastructure:itintegration:registry:2\" xmlns:urn1=\"urn:riv:infrastructure:itintegration:registry:GetSupportedServiceContractsResponder:2\">\n"
      + "   <soapenv:Header>\n"
      + "      <urn:LogicalAddress>HttpProducer</urn:LogicalAddress>\n"
      + "   </soapenv:Header>\n"
      + "   <soapenv:Body>\n"
      + "      <urn1:GetSupportedServiceContracts>\n"
      + "         <urn1:serviceConsumerHsaId>5565594230</urn1:serviceConsumerHsaId>\n"
      + "         <urn1:logicalAdress>5565594230</urn1:logicalAdress>\n"
      + "      </urn1:GetSupportedServiceContracts>\n"
      + "   </soapenv:Body>\n"
      + "</soapenv:envelope>\n"
      + "------=_Part_11_8757200.1571821395690--\n";

  public static final String EXPECTED_RESULT_2 = "<soapenv:envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:riv:infrastructure:itintegration:registry:2\" xmlns:urn1=\"urn:riv:infrastructure:itintegration:registry:GetSupportedServiceContractsResponder:2\">\n"
      + "   <soapenv:Header>\n"
      + "      <urn:LogicalAddress>HttpProducer</urn:LogicalAddress>\n"
      + "   </soapenv:Header>\n"
      + "   <soapenv:Body>\n"
      + "      <urn1:GetSupportedServiceContracts>\n"
      + "         <urn1:serviceConsumerHsaId>5565594230</urn1:serviceConsumerHsaId>\n"
      + "         <urn1:logicalAdress>5565594230</urn1:logicalAdress>\n"
      + "      </urn1:GetSupportedServiceContracts>\n"
      + "   </soapenv:Body>\n"
      + "</soapenv:envelope>";

  @Test
  public void extractXmlPayload() {
    RequestReaderProcessorXMLEventReader requestReader = new RequestReaderProcessorXMLEventReader();
    String mtomText = MTOM_TEXT_1;
    String xml = requestReader.extractXmlPayload(mtomText);
    assertEquals(EXPECTED_RESULT_1,xml);
  }

  @Test
  public void extractXmlPayload2() {
    RequestReaderProcessorXMLEventReader requestReader = new RequestReaderProcessorXMLEventReader();
    String mtomText = MTOM_TEXT_2;
    String xml = requestReader.extractXmlPayload(mtomText);
    assertEquals(EXPECTED_RESULT_2,xml);
  }

  @Test
  public void extractXmlPayloadFail() {
    RequestReaderProcessorXMLEventReader requestReader = new RequestReaderProcessorXMLEventReader();
    String mtomText = "This will fail";
    try {
      requestReader.extractXmlPayload(mtomText);
      fail("Expected a VpTechnicalException");
    } catch(VpTechnicalException e){
      assertEquals("Failed to extract XML part from MTOM message", e.getMessage());
    }catch (Exception e){
      fail("Expected a VpTechnicalException");
    }
  }
}