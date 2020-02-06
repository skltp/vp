package se.skl.tp.vp.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class VPStringUtil {
  private VPStringUtil() {}

  public static String concat(String... strings) {
    StringBuilder sb = new StringBuilder();
    for (String s : strings) {
      sb.append(s);
    }
    return sb.toString();
  }

  /**
   * @return true if candidate is neither of " ","" , null, "\t",System.lineSeparator() or any
   *     combination of these
   */
  public static boolean hasANonEmptyValue(String candidate) {

    return candidate != null && !"".equals(candidate.trim());
  }

  /**
   * @return true if candidate is any of " ","" , null, "\t",System.lineSeparator() or any
   *     combination of these
   */
  public static boolean valueIsEmpty(String candidate) {
    return !hasANonEmptyValue(candidate);
  }

  /**
   * Note that method don't do any attempts to convert any input. Only tested with UTF8 encoded
   * inputStream other uses are undefined.
   *
   * @param inputStream UTF8 Encoded
   */
  public static String inputStream2UTF8Str(InputStream inputStream) throws IOException {
    return inputStreamStr(inputStream, StandardCharsets.UTF_8);
  }

  private static String inputStreamStr(InputStream inputStream, Charset charset)
      throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
      return br.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }
}
