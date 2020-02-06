package se.skl.tp.vp.testutil;

import static org.junit.Assert.assertTrue;
import static se.skl.tp.vp.testutil.VPStringUtil.concat;
import static se.skl.tp.vp.testutil.VPStringUtil.hasANonEmptyValue;
import static se.skl.tp.vp.testutil.VPStringUtil.inputStream2UTF8Str;
import static se.skl.tp.vp.testutil.VPStringUtil.valueIsEmpty;
import static se.skl.tp.vp.wsdl.PathHelper.PATH_PREFIX;
import static se.skl.tp.vp.wsdl.PathHelper.getPath;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Test;

public class VPStringUtilTest {

  @Test
  public void testConcat() {
    assertTrue(concat("h", "e", "j", " världen är allt okej").matches("hej världen är allt okej"));
  }

  @Test
  public void testHasANonEmptyValue() {
    assertTrue(
        hasANonEmptyValue("hej världen är allt okej")
            && !hasANonEmptyValue(null)
            && !hasANonEmptyValue("")
            && !hasANonEmptyValue(" ")
            && hasANonEmptyValue(" A ")
            && hasANonEmptyValue("B ")
            && hasANonEmptyValue(" C"));
  }

  @Test
  public void testvalueIsEmpty() {
    assertTrue(
        !valueIsEmpty("hej världen är allt okej")
            && valueIsEmpty(null)
            && valueIsEmpty("")
            && valueIsEmpty(" ")
            && !valueIsEmpty(" A ")
            && !valueIsEmpty("B ")
            && !valueIsEmpty(" C")
            && valueIsEmpty(concat("\t",System.lineSeparator()," "))
            && !valueIsEmpty(concat("\t",System.lineSeparator(),"oj"," ")));
  }

  @Test
  public void testInputStream2UTF8Str() throws IOException, URISyntaxException {

     String s  = inputStream2UTF8Str(new FileInputStream(getPath(concat(PATH_PREFIX,"UTF8.txt")).toFile()));
    assertTrue(s.equals("Åäö hej"));
  }


}
