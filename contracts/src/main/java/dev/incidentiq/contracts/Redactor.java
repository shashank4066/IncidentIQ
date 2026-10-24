package dev.incidentiq.contracts;
import java.util.regex.Pattern;
/** Defense in depth before storing logs or sending them to a configured AI provider. */
public final class Redactor {
 private static final Pattern SECRET=Pattern.compile("(?i)(password|passwd|secret|api[_-]?key|access[_-]?token|authorization)([\\s\"':=]+)([^\\s,;\"}]+)");
 private static final Pattern BEARER=Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+");
 private static final Pattern EMAIL=Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
 private Redactor() {}
 public static String clean(String input,int max) {
  String value=clean(input);if(value.length()<=max)return value;
  if(max>0&&Character.isHighSurrogate(value.charAt(max-1)))max--;
  return value.substring(0,max);
 }
 public static String clean(String input) {
  if(input==null) return "";
  String value=BEARER.matcher(input).replaceAll("Bearer [REDACTED]");
  value=SECRET.matcher(value).replaceAll("$1$2[REDACTED]");
  return EMAIL.matcher(value).replaceAll("[EMAIL]");
 }
}
