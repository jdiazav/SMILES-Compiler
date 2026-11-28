package smiles;

import java.util.List;

import smiles.ast.Ast.BondOrder;
import smiles.ast.Ast.Molecule;
import smiles.ast.AstPrinter;
import smiles.graph.GraphBuilder;
import smiles.graph.MolecularGraph;
import smiles.lexer.Lexer;
import smiles.lexer.Lexer.LexicalException;
import smiles.lexer.Lexer.Token;
import smiles.parser.Parser;
import smiles.parser.Parser.ParseException;

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
            List<Token> tokens = new Lexer(smiles).tokenize();
            printTokens(tokens);

            Molecule ast = new Parser(tokens).parse();
            printSyntaxOk(ast);

            MolecularGraph graph = GraphBuilder.build(ast);
            printGraph(graph);
        } catch (LexicalException e) {
            printError(smiles, e.getMessage(), e.getPosition());
            System.exit(1);
        } catch (ParseException e) {
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

    private static void printSyntaxOk(Molecule ast) {
        System.out.println();
        System.out.println("== Fase 2: análisis sintáctico ==");
        System.out.println("Sintaxis correcta. AST construido:");
        System.out.println();
        System.out.println(AstPrinter.print(ast));
    }

    // Numeramos átomos y columnas desde 1 para que coincida con la salida de las fases anteriores
    private static void printGraph(MolecularGraph graph) {
        System.out.println();
        System.out.println("== Fase 3: grafo molecular ==");
        System.out.println(graph.atoms().size() + " átomos, " + graph.bonds().size() + " enlaces");

        System.out.println();
        System.out.println("Átomos (nodos):");
        System.out.printf("%-6s %-9s %-8s %-6s %-11s %s%n",
                "Átomo", "Elemento", "Columna", "Grado", "Σ órdenes", "Vecinos");
        for (MolecularGraph.Atom a : graph.atoms()) {
            StringBuilder neighbors = new StringBuilder();
            for (int n : graph.neighbors(a.index())) {
                if (neighbors.length() > 0) {
                    neighbors.append(", ");
                }
                neighbors.append(label(graph.atom(n)));
            }
            System.out.printf("%-6s %-9s %-8d %-6d %-11d %s%n",
                    label(a), a.element(), a.position() + 1,
                    graph.degree(a.index()), graph.bondOrderSum(a.index()),
                    neighbors.length() == 0 ? "-" : neighbors);
        }

        if (!graph.bonds().isEmpty()) {
            System.out.println();
            System.out.println("Enlaces (aristas):");
            System.out.printf("%-4s %-8s %s%n", "#", "Orden", "Átomos");
            int i = 1;
            for (MolecularGraph.Bond b : graph.bonds()) {
                String symbol = b.order() == BondOrder.DOUBLE ? "=" : "-";
                System.out.printf("%-4d %-8s %s %s %s%n", i++, b.order(),
                        label(graph.atom(b.from())), symbol, label(graph.atom(b.to())));
            }
        }
    }

    // Formato corto para imprimir el átomo, ej: "O3"
    private static String label(MolecularGraph.Atom a) {
        return a.element() + (a.index() + 1);
    }

    // Imprime el mensaje y marca la columna del error con '^'
    private static void printError(String input, String message, int position) {
        System.err.println();
        System.err.println(message);
        System.err.println("  " + input);
        System.err.println("  " + " ".repeat(position) + "^");
    }
}