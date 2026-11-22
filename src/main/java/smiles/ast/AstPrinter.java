package smiles.ast;

import java.util.ArrayList;
import java.util.List;

import smiles.ast.Ast.Atom;
import smiles.ast.Ast.Bond;
import smiles.ast.Ast.Branch;
import smiles.ast.Ast.BranchedAtom;
import smiles.ast.Ast.Chain;
import smiles.ast.Ast.Link;
import smiles.ast.Ast.Molecule;
import smiles.ast.Ast.Node;
import smiles.ast.Ast.Visitor;

/**
 * Imprime el AST en formato de árbol de texto.
 */
public final class AstPrinter implements Visitor<List<String>> {

    private AstPrinter() {
    }

    public static String print(Node node) {
        return String.join(System.lineSeparator(), node.accept(new AstPrinter()));
    }

    @Override
    public List<String> visitMolecule(Molecule node) {
        return tree("Molecule", List.of(node.chain()));
    }

    @Override
    public List<String> visitChain(Chain node) {
        List<Node> children = new ArrayList<>();
        children.add(node.first());
        children.addAll(node.links());
        return tree("Chain", children);
    }

    @Override
    public List<String> visitLink(Link node) {
        return tree("Link", List.of(node.bond(), node.atom()));
    }

    @Override
    public List<String> visitBranchedAtom(BranchedAtom node) {
        List<Node> children = new ArrayList<>();
        children.add(node.atom());
        children.addAll(node.branches());
        return tree("BranchedAtom", children);
    }

    @Override
    public List<String> visitBranch(Branch node) {
        return tree("Branch", List.of(node.bond(), node.chain()));
    }

    @Override
    public List<String> visitBond(Bond node) {
        String label = "Bond " + node.order();
        if (node.explicit()) {
            String symbol = node.order() == Ast.BondOrder.DOUBLE ? "=" : "-";
            label += " '" + symbol + "'" + column(node.position());
        } else {
            label += " (implícito)";
        }
        return List.of(label);
    }

    @Override
    public List<String> visitAtom(Atom node) {
        return List.of("Atom " + node.element() + column(node.position()));
    }

    // Arma las líneas del nodo actual y le agrega la indentación/ramas a cada hijo
    private List<String> tree(String label, List<? extends Node> children) {
        List<String> lines = new ArrayList<>();
        lines.add(label);
        for (int i = 0; i < children.size(); i++) {
            boolean last = i == children.size() - 1;
            List<String> child = children.get(i).accept(this);
            lines.add((last ? "└── " : "├── ") + child.get(0));
            String indent = last ? "    " : "│   ";
            for (String line : child.subList(1, child.size())) {
                lines.add(indent + line);
            }
        }
        return lines;
    }

    // Las posiciones del AST empiezan en 0; para mostrar se suman 1
    private static String column(int position) {
        return "  [col " + (position + 1) + "]";
    }
}
