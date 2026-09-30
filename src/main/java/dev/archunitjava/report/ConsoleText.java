package dev.archunitjava.report;

/** Single-line terminal escaping shared by human-readable reports and logging. */
public final class ConsoleText {
    private ConsoleText() {}

    public static String sanitize(String value) {
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (codePoint == '\n') out.append("\\n");
            else if (codePoint == '\r') out.append("\\r");
            else if (codePoint == '\t') out.append("\\t");
            else if (Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.FORMAT
                    || codePoint == 0x2028 || codePoint == 0x2029) out.append('?');
            else out.appendCodePoint(codePoint);
        });
        return out.toString();
    }

    public static String style(String text, String ansiCode, boolean color) {
        return color ? "\u001b[" + ansiCode + "m" + text + "\u001b[0m" : text;
    }
}
