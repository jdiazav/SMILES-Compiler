package smiles.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Analizador léxico para el subconjunto SMILES soportado.
 */
public final class Lexer {

    public enum TokenType {
        ATOM,   // C, O, N
        BOND,   // -, =
        LPAREN, // (
        RPAREN, // )
        EOF
    }

    public record Token(TokenType type, String lexeme, int position) {

        @Override
        public String toString() {
            return switch (type) {
                case ATOM, BOND -> type + "(" + lexeme + ")";
                default -> type.name();
            };
        }
    }

    public static class LexicalException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        private final int position;

        public LexicalException(String detail, int position) {
            super("Error léxico en la columna " + (position + 1) + ": " + detail);
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }

    private final String input;
    private int pos;

    public Lexer(String input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Tokeniza toda la entrada y añade EOF al final.
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        pos = 0;
        while (pos < input.length()) {
            tokens.add(nextToken());
        }
        tokens.add(new Token(TokenType.EOF, "", pos));
        return tokens;
    }

    private Token nextToken() {
        int start = pos;
        char c = input.charAt(pos);
        return switch (c) {
            case 'C' -> {
                // Evita leer "Cl" como carbono seguido de una 'l' inválida
                if (lookahead(1) == 'l') {
                    throw new LexicalException("el cloro 'Cl' no está soportado (solo C, O y N)", start);
                }
                yield emit(TokenType.ATOM, start);
            }
            case 'O', 'N' -> emit(TokenType.ATOM, start);
            case '-', '=' -> emit(TokenType.BOND, start);
            case '(' -> emit(TokenType.LPAREN, start);
            case ')' -> emit(TokenType.RPAREN, start);
            default -> throw new LexicalException(describeInvalid(c), start);
        };
    }

    private Token emit(TokenType type, int start) {
        pos++;
        return new Token(type, input.substring(start, pos), start);
    }

    private char lookahead(int k) {
        int i = pos + k;
        return i < input.length() ? input.charAt(i) : '\0';
    }

    // Mensajes específicos para sintaxis SMILES real que queda fuera del alcance definido
    private String describeInvalid(char c) {
        if (c == 'B' && lookahead(1) == 'r') {
            return "el bromo 'Br' no está soportado (solo C, O y N)";
        }
        if (c == 'c' || c == 'n' || c == 'o' || c == 's' || c == 'p' || c == 'b') {
            return "los átomos aromáticos ('" + c + "' en minúscula) no están soportados";
        }
        if (Character.isDigit(c) || c == '%') {
            return "los cierres de anillo ('" + c + "') no están soportados";
        }
        if (Character.isWhitespace(c)) {
            return "no se permiten espacios dentro de la cadena SMILES";
        }
        return switch (c) {
            case '#' -> "el enlace triple '#' no está soportado";
            case '[', ']' -> "los átomos entre corchetes ('" + c + "') no están soportados";
            case '.' -> "las moléculas desconectadas ('.') no están soportadas";
            case '/', '\\' -> "la estereoquímica de enlaces ('" + c + "') no está soportada";
            default -> "carácter no reconocido '" + c + "'";
        };
    }
}
