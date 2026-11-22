package smiles.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import smiles.ast.Ast.Atom;
import smiles.ast.Ast.Bond;
import smiles.ast.Ast.BondOrder;
import smiles.ast.Ast.Branch;
import smiles.ast.Ast.BranchedAtom;
import smiles.ast.Ast.Chain;
import smiles.ast.Ast.Link;
import smiles.ast.Ast.Molecule;
import smiles.lexer.Lexer.Token;
import smiles.lexer.Lexer.TokenType;

/**
 * Parser descendente recursivo LL(1) para el subconjunto SMILES.
 */
public final class Parser {

    public static class ParseException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final int position;

        public ParseException(String detail, int position) {
            super("Error sintáctico en la columna " + (position + 1) + ": " + detail);
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }

    private final List<Token> tokens;
    private int current;

    public Parser(List<Token> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty() || tokens.get(tokens.size() - 1).type() != TokenType.EOF) {
            throw new IllegalArgumentException("la lista de tokens debe terminar en EOF");
        }
        this.tokens = tokens;
    }

    public Molecule parse() {
        current = 0;
        return parseMolecule();
    }

    private Molecule parseMolecule() {
        if (check(TokenType.EOF)) {
            throw error("la cadena SMILES está vacía", peek());
        }
        Chain chain = parseChain("la molécula debe empezar con un átomo");
        if (!check(TokenType.EOF)) {
            Token t = peek();
            if (t.type() == TokenType.RPAREN) {
                throw error("')' sin un '(' que lo abra", t);
            }
            throw error("se encontró " + describe(t) + " después del final de la molécula", t);
        }
        return new Molecule(chain);
    }

    private Chain parseChain(String expectedAtomMessage) {
        if (!check(TokenType.ATOM)) {
            throw error(expectedAtomMessage + ", pero se encontró " + describe(peek()), peek());
        }
        BranchedAtom first = parseBranchedAtom();
        List<Link> links = new ArrayList<>();

        while (check(TokenType.BOND) || check(TokenType.ATOM)) {
            Bond bond = parseBondOpt();
            if (!check(TokenType.ATOM)) {
                throw error("se esperaba un átomo después del enlace '"
                        + previous().lexeme() + "', pero se encontró " + describe(peek()), peek());
            }
            links.add(new Link(bond, parseBranchedAtom()));
        }
        return new Chain(first, links);
    }

    private BranchedAtom parseBranchedAtom() {
        Atom atom = parseAtom();
        List<Branch> branches = new ArrayList<>();
        while (check(TokenType.LPAREN)) {
            branches.add(parseBranch());
        }
        return new BranchedAtom(atom, branches);
    }

    private Branch parseBranch() {
        Token open = advance(); // '('
        if (check(TokenType.RPAREN)) {
            throw error("rama vacía '()'", peek());
        }
        Bond bond = parseBondOpt();
        if (bond.explicit() && !check(TokenType.ATOM)) {
            throw error("se esperaba un átomo después del enlace '"
                    + previous().lexeme() + "', pero se encontró " + describe(peek()), peek());
        }
        Chain chain = parseChain("una rama debe empezar con un átomo");
        if (!check(TokenType.RPAREN)) {
            throw error("falta ')' para cerrar el '(' de la columna " + (open.position() + 1), peek());
        }
        advance(); // ')'
        return new Branch(bond, chain);
    }

    // Si no viene enlace explícito, se asume enlace simple implícito
    private Bond parseBondOpt() {
        if (check(TokenType.BOND)) {
            Token t = advance();
            BondOrder order = t.lexeme().equals("=") ? BondOrder.DOUBLE : BondOrder.SINGLE;
            return new Bond(order, true, t.position());
        }
        return Bond.implicitSingle(peek().position());
    }

    private Atom parseAtom() {
        Token t = advance();
        return new Atom(t.lexeme(), t.position());
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        Token t = peek();
        if (t.type() != TokenType.EOF) {
            current++;
        }
        return t;
    }

    private static ParseException error(String detail, Token at) {
        return new ParseException(detail, at.position());
    }

    private static String describe(Token t) {
        return switch (t.type()) {
            case EOF -> "el final de la cadena";
            case ATOM -> "el átomo '" + t.lexeme() + "'";
            case BOND -> "el enlace '" + t.lexeme() + "'";
            case LPAREN -> "'('";
            case RPAREN -> "')'";
        };
    }
}
