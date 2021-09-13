package se.skl.tp.vp.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JunitUtil {

	public static void assertStringContains(String a, String b) {
		assertTrue(a.contains(b));
	}
}
