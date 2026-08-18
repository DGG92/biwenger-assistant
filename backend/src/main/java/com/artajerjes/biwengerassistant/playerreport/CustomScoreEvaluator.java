package com.artajerjes.biwengerassistant.playerreport;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CustomScoreEvaluator {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    public int evaluate(
            String expression,
            Map<String, Object> variables) {

        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException(
                    "Custom score expression cannot be empty");
        }

        Parser parser = new Parser(
                expression,
                variables == null
                        ? Map.of()
                        : variables);

        BigDecimal result = parser.parse();

        /*
         * Reproduce el comportamiento que ya habíamos observado
         * en Biwenger:
         *
         * 1.5 -> 2
         * -1.5 -> -2
         *
         * HALF_UP redondea los .5 alejándose de cero.
         */
        return result
                .setScale(
                        0,
                        RoundingMode.HALF_UP)
                .intValue();
    }

    private static final class Parser {

        private final String expression;
        private final Map<String, Object> variables;

        private int position;

        private Parser(
                String expression,
                Map<String, Object> variables) {

            this.expression = expression;
            this.variables = variables;
        }

        private BigDecimal parse() {

            BigDecimal result = parseOr();

            skipWhitespace();

            if (!isAtEnd()) {
                throw error(
                        "Unexpected token");
            }

            return result;
        }

        /*
         * Precedencia:
         *
         * or
         * and
         * comparaciones
         * + -
         * * /
         * unarios
         * primarios
         */

        private BigDecimal parseOr() {

            BigDecimal left = parseAnd();

            while (true) {

                skipWhitespace();

                if (!matchKeyword("or")) {
                    return left;
                }

                BigDecimal right = parseAnd();

                left = booleanValue(
                        asBoolean(left)
                                || asBoolean(right));
            }
        }

        private BigDecimal parseAnd() {

            BigDecimal left = parseComparison();

            while (true) {

                skipWhitespace();

                if (!matchKeyword("and")) {
                    return left;
                }

                BigDecimal right = parseComparison();

                left = booleanValue(
                        asBoolean(left)
                                && asBoolean(right));
            }
        }

        private BigDecimal parseComparison() {

            BigDecimal left = parseAddition();

            while (true) {

                skipWhitespace();

                if (match(">=")) {

                    BigDecimal right = parseAddition();

                    left = booleanValue(
                            left.compareTo(right) >= 0);

                } else if (match("<=")) {

                    BigDecimal right = parseAddition();

                    left = booleanValue(
                            left.compareTo(right) <= 0);

                } else if (match("==")) {

                    BigDecimal right = parseAddition();

                    left = booleanValue(
                            left.compareTo(right) == 0);

                } else if (match("!=")) {

                    BigDecimal right = parseAddition();

                    left = booleanValue(
                            left.compareTo(right) != 0);

                } else if (match(">")) {

                    BigDecimal right = parseAddition();

                    left = booleanValue(
                            left.compareTo(right) > 0);

                } else if (match("<")) {

                    BigDecimal right = parseAddition();

                    left = booleanValue(
                            left.compareTo(right) < 0);

                } else {

                    return left;
                }
            }
        }

        private BigDecimal parseAddition() {

            BigDecimal left = parseMultiplication();

            while (true) {

                skipWhitespace();

                if (match("+")) {

                    BigDecimal right = parseMultiplication();

                    left = left.add(
                            right,
                            MATH_CONTEXT);

                } else if (match("-")) {

                    BigDecimal right = parseMultiplication();

                    left = left.subtract(
                            right,
                            MATH_CONTEXT);

                } else {

                    return left;
                }
            }
        }

        private BigDecimal parseMultiplication() {

            BigDecimal left = parseUnary();

            while (true) {

                skipWhitespace();

                if (match("*")) {

                    BigDecimal right = parseUnary();

                    left = left.multiply(
                            right,
                            MATH_CONTEXT);

                } else if (match("/")) {

                    BigDecimal right = parseUnary();

                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        throw error(
                                "Division by zero");
                    }

                    left = left.divide(
                            right,
                            MATH_CONTEXT);

                } else {

                    return left;
                }
            }
        }

        private BigDecimal parseUnary() {

            skipWhitespace();

            if (match("+")) {
                return parseUnary();
            }

            if (match("-")) {
                return parseUnary().negate(
                        MATH_CONTEXT);
            }

            return parsePrimary();
        }

        private BigDecimal parsePrimary() {

            skipWhitespace();

            if (match("(")) {

                BigDecimal result = parseOr();

                skipWhitespace();

                expect(")");

                return result;
            }

            if (isDigit(peek())
                    || peek() == '.') {

                return parseNumber();
            }

            if (isIdentifierStart(peek())) {

                String identifier = parseIdentifier();

                skipWhitespace();

                if ("if".equals(identifier)
                        && match("(")) {

                    return parseIf();
                }

                if ("true".equals(identifier)) {
                    return BigDecimal.ONE;
                }

                if ("false".equals(identifier)) {
                    return BigDecimal.ZERO;
                }

                return resolveVariable(
                        identifier);
            }

            throw error(
                    "Expected number, variable, function or parenthesis");
        }

        private BigDecimal parseIf() {

            BigDecimal condition = parseOr();

            skipWhitespace();
            expect(",");

            BigDecimal trueValue = parseOr();

            skipWhitespace();

            /*
             * Biwenger utiliza habitualmente:
             *
             * if(condicion, valor)
             *
             * cuyo false implícito es 0.
             *
             * Admitimos también un tercer parámetro por robustez:
             *
             * if(condicion, valorSiTrue, valorSiFalse)
             */
            BigDecimal falseValue = BigDecimal.ZERO;

            if (match(",")) {
                falseValue = parseOr();
            }

            skipWhitespace();
            expect(")");

            return asBoolean(condition)
                    ? trueValue
                    : falseValue;
        }

        private BigDecimal parseNumber() {

            int start = position;

            boolean decimalPointSeen = false;

            while (!isAtEnd()) {

                char current = peek();

                if (isDigit(current)) {

                    position++;
                    continue;
                }

                if (current == '.'
                        && !decimalPointSeen) {

                    decimalPointSeen = true;
                    position++;
                    continue;
                }

                break;
            }

            String number = expression.substring(
                    start,
                    position);

            if (".".equals(number)) {
                throw error(
                        "Invalid number");
            }

            try {
                return new BigDecimal(
                        number,
                        MATH_CONTEXT);

            } catch (NumberFormatException exception) {

                throw error(
                        "Invalid number: " + number);
            }
        }

        private String parseIdentifier() {

            int start = position;

            position++;

            while (!isAtEnd()
                    && isIdentifierPart(peek())) {

                position++;
            }

            return expression.substring(
                    start,
                    position);
        }

        private BigDecimal resolveVariable(
                String identifier) {

            Object value = variables.get(identifier);

            /*
             * Las estadísticas que no aparecen en rawStats
             * se consideran 0 / false.
             *
             * Ejemplo:
             *
             * Ryan no tiene "goals" en rawStats.
             *
             * goals > 0
             *
             * debe evaluarse como false, no provocar un error.
             */
            if (value == null) {
                return BigDecimal.ZERO;
            }

            if (value instanceof Boolean booleanValue) {

                return booleanValue
                        ? BigDecimal.ONE
                        : BigDecimal.ZERO;
            }

            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }

            if (value instanceof Number number) {

                return new BigDecimal(
                        number.toString(),
                        MATH_CONTEXT);
            }

            if (value instanceof String stringValue) {

                if ("true".equalsIgnoreCase(
                        stringValue)) {

                    return BigDecimal.ONE;
                }

                if ("false".equalsIgnoreCase(
                        stringValue)) {

                    return BigDecimal.ZERO;
                }

                try {
                    return new BigDecimal(
                            stringValue,
                            MATH_CONTEXT);

                } catch (NumberFormatException exception) {

                    throw error(
                            "Variable '"
                                    + identifier
                                    + "' is not numeric or boolean");
                }
            }

            throw error(
                    "Unsupported value for variable '"
                            + identifier
                            + "'");
        }

        private boolean asBoolean(
                BigDecimal value) {

            return value.compareTo(
                    BigDecimal.ZERO) != 0;
        }

        private BigDecimal booleanValue(
                boolean value) {

            return value
                    ? BigDecimal.ONE
                    : BigDecimal.ZERO;
        }

        private void skipWhitespace() {

            while (!isAtEnd()
                    && Character.isWhitespace(
                            expression.charAt(position))) {

                position++;
            }
        }

        private boolean match(
                String token) {

            skipWhitespace();

            if (!expression.startsWith(
                    token,
                    position)) {

                return false;
            }

            position += token.length();

            return true;
        }

        private boolean matchKeyword(
                String keyword) {

            skipWhitespace();

            if (!expression.startsWith(
                    keyword,
                    position)) {

                return false;
            }

            int end = position
                    + keyword.length();

            if (position > 0
                    && isIdentifierPart(
                            expression.charAt(position - 1))) {

                return false;
            }

            if (end < expression.length()
                    && isIdentifierPart(
                            expression.charAt(end))) {

                return false;
            }

            position = end;

            return true;
        }

        private void expect(
                String token) {

            if (!match(token)) {
                throw error(
                        "Expected '" + token + "'");
            }
        }

        private char peek() {

            if (isAtEnd()) {
                return '\0';
            }

            return expression.charAt(
                    position);
        }

        private boolean isAtEnd() {

            return position >= expression.length();
        }

        private boolean isDigit(
                char value) {

            return value >= '0'
                    && value <= '9';
        }

        private boolean isIdentifierStart(
                char value) {

            return Character.isLetter(value)
                    || value == '_';
        }

        private boolean isIdentifierPart(
                char value) {

            return Character.isLetterOrDigit(value)
                    || value == '_';
        }

        private IllegalArgumentException error(
                String message) {

            return new IllegalArgumentException(
                    message
                            + " at position "
                            + position
                            + " in custom score expression");
        }
    }
}