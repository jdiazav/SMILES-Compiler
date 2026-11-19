package smiles;

import java.util.List;

import smiles.lexer.Lexer;
import smiles.lexer.Lexer.LexicalException;
import smiles.lexer.Lexer.Token;

/**
 * Punto de entrada del compilador de SMILES.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -jar target/smiles-compiler.jar \"<cadena SMILES>\"");
            System.exit(1);
        }
        String smiles = args[0];
        System.out.println("Entrada SMILES: " + smiles);

        try {
            // Por ahora solo está conectado el lexer; luego irán el parser y el resto de fases
            List<Token> tokens = new Lexer(smiles).tokenize();
            printTokens(tokens);
        } catch (LexicalException e) {
            printError(smiles, e.getMessage(), e.getPosition());
            System.exit(1);
        }
    }

    private static void printTokens(List<Token> tokens) {
        System.out.println();
        System.out.println("== Fase 1: análisis léxico ==");
        System.out.printf("%-4s %-8s %-7s %s%n", "#", "Tipo", "Lexema", "Columna");
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            System.out.printf("%-4d %-8s %-7s %d%n", i + 1, t.type(), t.lexeme(), t.position() + 1);
        }
        System.out.println();
        StringBuilder sequence = new StringBuilder("Secuencia:");
        for (Token t : tokens) {
            sequence.append(' ').append(t);
        }
        System.out.println(sequence);
    }

    // Imprime el mensaje y marca la columna del error con '^'
    private static void printError(String input, String message, int position) {
        System.err.println();
        System.err.println(message);
        System.err.println("  " + input);
        System.err.println("  " + " ".repeat(position) + "^");
    }
}
