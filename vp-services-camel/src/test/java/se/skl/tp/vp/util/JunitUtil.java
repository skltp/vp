package se.skl.tp.vp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JunitUtil {

	public static void assertStringContains(String a, String b) {
		assertTrue(a.contains(b));
	}

	public static void assertMatchRegexGroup(String actual, String pattern, String expected) {
		assertMatchRegexGroup(actual,pattern,expected,0);
	}
	public static void assertMatchRegexGroup(String actual, String pattern, String expected, Object group) {
		try {
			Pattern compiledPattern = Pattern.compile(pattern);
			Matcher matcher = compiledPattern.matcher(actual);

			matcher.find();


			String actualValue = "";
			if (group instanceof Integer) {
				actualValue = matcher.group((Integer) group);
			} else if (group instanceof String) {
				actualValue = matcher.group((String) group);
			} else {
				throw new AssertionError("Pattern didn't match any group");
			}
			assertEquals(expected, actualValue);
		} catch (IllegalStateException e) {
			throw new AssertionError("Pattern didn't match any group");
		}
	}
}
