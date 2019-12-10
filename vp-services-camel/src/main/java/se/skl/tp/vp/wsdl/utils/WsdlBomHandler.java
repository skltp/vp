package se.skl.tp.vp.wsdl.utils;

public class WsdlBomHandler {
  /**
   * Removes any "garbage" chars that might appear before the <?xml version="XX" encoding="ENCODING"
   * ?> that otherwise will make DocumentHelper.parseText throw an * exception. As i understand
   * https://www.w3.org/TR/xml/#charencoding byte order mark is Optional for UTF-8 and mandatory
   * UTF-16. One could therefore argue that parseText should handel the byte order mark, on the
   * other hand it also states that if no byte order mark is included the file must start/include
   * with a encoding declaration.
   *
   * <p>This implies it is safe to remove any chars including the bom as long as long the wsdl
   * includes a encoding declaration (as for 2019-11-28 all current wsdl:s do).
   *
   * <p>The method assumes that any wsdl "starting" with "<?xml" will also include a encoding
   * attribute and that it hense is safe to remove bom. You could dream up scenarios where it would
   * be safer to check string between <? and ?> include any known valid encoding etc. however any
   * such scenarios seems academic enough to not be worth the overhead.
   */
  private WsdlBomHandler() {}

  public static String removeCharsBeforeXmlDeclaration(String s) {
    int index = s.indexOf("<?xml");
    if (index > 0 && index < 10) {
      s = s.substring(index);
    }

    return s;
  }
}
