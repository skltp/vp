package se.skl.tp.vp.wsdl.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.xml.WSDLReader;
import lombok.extern.log4j.Log4j2;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.CatalogWSDLLocator;
import org.apache.cxf.wsdl11.ResourceManagerWSDLLocator;

@Log4j2
public class WsdlDefinitionHandler {

  Bus bus;
  WSDLReader wsdlReader;

  public WsdlDefinitionHandler() {
    bus = BusFactory.getDefaultBus();
    WSDLManager wsdlManager = bus.getExtension(WSDLManager.class);
    wsdlReader = wsdlManager.getWSDLFactory().newWSDLReader();
    wsdlReader.setFeature("javax.wsdl.verbose", false);
    wsdlReader.setFeature("javax.wsdl.importDocuments", true);
    wsdlReader.setExtensionRegistry(wsdlManager.getExtensionRegistry());
  }

  public Definition getWsdlDefinition(String wsdlFileName) throws WSDLException {
    CatalogWSDLLocator catLocator = new CatalogWSDLLocator(wsdlFileName, bus);
    ResourceManagerWSDLLocator wsdlLocator = new ResourceManagerWSDLLocator(wsdlFileName, catLocator, bus);
    return wsdlReader.readWSDL(wsdlLocator);
  }

  public Map<String, SchemaReference> getAllSchemaReferences(String wsdlFile) throws WSDLException {
    Definition getWsdlDefinition = getWsdlDefinition(wsdlFile);
    Map<String, SchemaReference> schemaReferenceMap = new HashMap<>();
    Types types = getWsdlDefinition.getTypes();
    if (types != null) {
      for (ExtensibilityElement extensibilityElement
          : CastUtils.cast(types.getExtensibilityElements(), ExtensibilityElement.class)) {
        if (extensibilityElement instanceof Schema) {
          Schema schema = (Schema) extensibilityElement;
          traverseAllSchemas(schema, schemaReferenceMap);
        }
      }
    }
    return schemaReferenceMap;
  }


  protected void traverseAllSchemas(Schema schema, Map<String, SchemaReference> doneSchemas) throws WSDLException {
    Collection<List<?>> imports = CastUtils.cast((Collection<?>) schema.getImports().values());
    for (List<?> lst : imports) {
      List<SchemaImport> schemaImportList = CastUtils.cast(lst);
      for (SchemaImport schemaImport : schemaImportList) {
        String schemaURI = schemaImport.getSchemaLocationURI();
        if (schemaURI != null && doneSchemas.put(decodeURL(schemaURI), schemaImport) == null) {
          traverseAllSchemas(schemaImport.getReferencedSchema(), doneSchemas);
        }
      }
    }
  }


  String decodeURL(String url) throws WSDLException {
    try {
      return URLDecoder.decode(url, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new WSDLException("OTHER_ERROR", e.getMessage());
    }
  }

}
