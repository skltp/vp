package se.skl.tp.vp.xmlutil;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static se.skl.tp.vp.wsdl.PathHelper.PATH_PREFIX;
import static se.skl.tp.vp.wsdl.PathHelper.getPath;
import static se.skl.tp.vp.wsdl.PathHelper.findFilesInDirectory;
import static se.skl.tp.vp.wsdl.PathHelper.findFoldersInDirectory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.dom4j.DocumentException;
import org.dom4j.XPath;
import org.junit.Before;
import org.junit.Test;
import se.skl.tp.vp.testutil.VPStringUtil;

public class XmlHelperAndPathHelperTest {

  private File happy = null;
  private File sad = null;

  @Before
  public void createXPath() throws IOException, URISyntaxException {

    List<File> folders =
        findFoldersInDirectory(
            getPath(VPStringUtil.concat(PATH_PREFIX, "xmlTests")).toString(),
            "withTestFiles");

    assumeTrue(folders.size() == 1);
    File xmlfolder = folders.get(0);

    List<File> files = findFilesInDirectory(xmlfolder.getPath(), ".*\\.xsd$");
    assumeTrue(files.size() == 3);
    sad = files.get(0);

    files = findFilesInDirectory(xmlfolder.getPath(), ".*\\.wsdl$");
    assumeTrue(files.size() == 2);
    happy = files.get(0);
  }

  @Test
  public void openDocSelectXPathStringValue() throws IOException, DocumentException, URISyntaxException {
    String s =
        XmlHelper.selectXPathStringValue(
            XmlHelper.openDocument(happy.getPath()),
            "wsdl:definitions/wsdl:types/xs:schema/xs:import/@namespace[contains(.,'Responder')]",
            "wsdl=http://schemas.xmlsoap.org/wsdl/",
            "soap=http://schemas.xmlsoap.org/wsdl/soap/",
            "xs=http://www.w3.org/2001/XMLSchema",
            "wsa=http://www.w3.org/2005/08/addressing",
            "tjsr=urn:riv:ehr:accesscontrol:AssertCareEngagementResponder:1",
            "tjsi=urn:riv:ehr:accesscontrol:AssertCareEngagementInitiator:1",
            "tns=urn:riv:ehr:accesscontrol:AssertCareEngagement:1:rivtabp20");
    assertTrue("urn:riv:ehr:accesscontrol:AssertCareEngagementResponder:1".equals(s));
  }

  @Test
  public void applyHandlingToNodes() throws IOException, DocumentException, URISyntaxException {
    AtomicInteger noOfHandlings = new AtomicInteger(0);
    XPath path =
        createTestXPath("wsdl:definitions/wsdl:types/xs:schema/xs:import/@namespace");
    XmlHelper.applyHandlingToNodes(
        XmlHelper.openDocument(happy.getPath()), path, node -> noOfHandlings.getAndIncrement());
    assertTrue(noOfHandlings.get()==2);

  }

  @Test
  public void openDocumentHappyXPath() throws IOException, DocumentException, URISyntaxException {
    XPath path =
        createTestXPath(
            "wsdl:definitions/wsdl:types/xs:schema/xs:import/@namespace[contains(.,'Responder')]");
    List<org.dom4j.Node> list = path.selectNodes(XmlHelper.openDocument(happy.getPath()));
    assertTrue(list.size() == 1);
  }

  @Test
  public void openDocumentSadXPath() throws IOException, DocumentException, URISyntaxException {
    XPath path =
        createTestXPath(
            "wsdl:definitions/wsdl:types/xs:schema/xs:import/@namespace[contains(.,'Responder')]");
    List<org.dom4j.Node> list = path.selectNodes(XmlHelper.openDocument(sad.getPath()));
    assertTrue(list.size() == 0);
  }

  private XPath createTestXPath(String s) {
    return XmlHelper.createXPath(
        s,
        "wsdl=http://schemas.xmlsoap.org/wsdl/",
        "soap=http://schemas.xmlsoap.org/wsdl/soap/",
        "xs=http://www.w3.org/2001/XMLSchema",
        "wsa=http://www.w3.org/2005/08/addressing",
        "tjsr=urn:riv:ehr:accesscontrol:AssertCareEngagementResponder:1",
        "tjsi=urn:riv:ehr:accesscontrol:AssertCareEngagementInitiator:1",
        "tns=urn:riv:ehr:accesscontrol:AssertCareEngagement:1:rivtabp20");
  }
}
