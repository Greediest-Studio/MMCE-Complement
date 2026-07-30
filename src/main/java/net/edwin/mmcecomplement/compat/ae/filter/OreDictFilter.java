package net.edwin.mmcecomplement.compat.ae.filter;

import java.util.Set;
import java.util.regex.Pattern;

/** Small boolean expression parser for ore-dict filter strings. */
public final class OreDictFilter {
    private OreDictFilter() {}

    public static boolean matches(String expression, Set<String> oreNames,
                                  boolean emptyValue) {
        if (expression == null || expression.trim().isEmpty()) return emptyValue;
        try {
            Parser parser = new Parser(expression, oreNames);
            boolean result = parser.parseOr();
            return parser.atEnd() ? result : false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static final class Parser {
        private final String input;
        private final Set<String> names;
        private int index;
        private Parser(String input, Set<String> names) {
            this.input = input;
            this.names = names;
        }
        private boolean parseOr() {
            boolean value = parseAnd();
            while (consume('|')) value |= parseAnd();
            return value;
        }
        private boolean parseAnd() {
            boolean value = parseNot();
            while (consume('&')) value &= parseNot();
            return value;
        }
        private boolean parseNot() {
            return consume('!') ? !parseNot() : atom();
        }
        private boolean atom() {
            skipSpaces();
            int start = index;
            while (index < input.length()) {
                char c = input.charAt(index);
                if (c == '|' || c == '&' || c == '!') break;
                index++;
            }
            String token = input.substring(start, index).trim();
            if (token.isEmpty()) throw new IllegalArgumentException("empty filter term");
            String regex = wildcardRegex(token);
            for (String name : names) if (Pattern.matches(regex, name)) return true;
            return false;
        }
        private boolean consume(char expected) {
            skipSpaces();
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }
        private void skipSpaces() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        }
        private boolean atEnd() { skipSpaces(); return index >= input.length(); }
    }

    private static String wildcardRegex(String token) {
        StringBuilder result = new StringBuilder("^");
        int start = 0;
        for (int i = 0; i <= token.length(); i++) {
            if (i == token.length() || token.charAt(i) == '*') {
                result.append(Pattern.quote(token.substring(start, i)));
                if (i < token.length()) result.append(".*");
                start = i + 1;
            }
        }
        return result.append('$').toString();
    }
}
