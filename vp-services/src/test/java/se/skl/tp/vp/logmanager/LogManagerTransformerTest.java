package se.skl.tp.vp.logmanager;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;
import org.soitoolkit.commons.mule.util.MiscUtil;

public class LogManagerTransformerTest {

	@Test
	public void testTransformer() throws IOException, TransformerException {

		// Read input and expected result from testfiles 
		String input          = MiscUtil.readFileAsString("src/test/resources/testfiles/LogManager-input.txt");
		String expectedResult = MiscUtil.readFileAsString("src/test/resources/testfiles/LogManager-expected-result.txt");
		
		// Create the transformer under test and let it perform the transformation
		LogManagerTransformer transformer = new LogManagerTransformer();
		String result = (String)transformer.doTransform(input, "UTF-8");

		// Compare the result to the expected value
		assertEquals(expectedResult, result);
	}

}