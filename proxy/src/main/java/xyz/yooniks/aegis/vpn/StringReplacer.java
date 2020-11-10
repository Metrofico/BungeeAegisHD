package xyz.yooniks.aegis.vpn;

public final class StringReplacer {

  private StringReplacer() {
  }

  public static String replace(String text, String searchString, String replacement) {
    if (text == null || text.isEmpty() || searchString.isEmpty()) {
      return text;
    }
    if (replacement == null) {
      replacement = "";
    }

    int start = 0;
    int max = -1;
    int end = text.indexOf(searchString, start);
    if (end == -1) {
      return text;
    }

    int replLength = searchString.length();
    int increase = replacement.length() - replLength;
    increase = (increase < 0 ? 0 : increase);
    increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
    StringBuilder sb = new StringBuilder(text.length() + increase);
    while (end != -1) {
      sb.append(text, start, end).append(replacement);
      start = end + replLength;
      if (--max == 0) {
        break;
      }
      end = text.indexOf(searchString, start);
    }
    sb.append(text.substring(start));
    return sb.toString();
  }

}
