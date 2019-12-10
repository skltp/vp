package se.skl.tp.vp.integrationtests.utils;

public class CaseRandomize {

  private static boolean getRandomBoolean() {
    return Math.random() < 0.5;
  }

  /**
   * Example pSource=HelloWorld may come out as helloWORLD or heLloworlD etc
   *
   * @param pSource pattern
   * @return as long as pSource is not empty and contains only letters the result used in the
   *     following boolean expression (pSource.equalsIgnoreCase(result)&&(!pSource.equals(result)))
   *     is always true. As the name implies the result may differ from time to time even if pSource
   *     is the same.
   */
  public static String randomCase(String pSource) {
    if (pSource == null || pSource.equals("")) {
      return pSource;
    }

    String[] fragmented = pSource.split("");
    StringBuilder tmpRes = new StringBuilder();
    int i = 0;
    for (String letter : fragmented) {
      if (getRandomBoolean() || (i == (fragmented.length / 2))) {
        char c = letter.charAt(0);
        if (Character.isUpperCase(c)) {
          tmpRes.append(letter.toLowerCase());
        } else {
          tmpRes.append(letter.toUpperCase());
        }
      } else {
        tmpRes.append(letter);
      }
      i++;
    }
    return tmpRes.toString();
  }
}
