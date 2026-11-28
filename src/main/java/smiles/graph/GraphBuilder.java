package smiles.graph;

import java.util.ArrayList;
import java.util.List;

import smiles.ast.Ast.Atom;
import smiles.ast.Ast.Bond;
import smiles.ast.Ast.BondOrder;
import smiles.ast.Ast.Branch;
import smiles.ast.Ast.BranchedAtom;
import smiles.ast.Ast.Chain;
import smiles.ast.Ast.Link;
import smiles.ast.Ast.Molecule;
import smiles.ast.Ast.Visitor;

/**
 * Convierte el AST en un MolecularGraph recorriendo el árbol de izquierda a derecha.
 * Cada visit devuelve el índice del primer átomo de su subárbol (al que se conecta
 * el enlace anterior). La validación química se hace después en el análisis semántico.
 */
public final class GraphBuilder implements Visitor<Integer> {

    private final List<MolecularGraph.Atom> atoms = new ArrayList<>();
    private final List<MolecularGraph.Bond> bonds = new ArrayList<>();
    // Átomo origen al que se conectará el siguiente Link o Branch
    private int origin = -1;

    private GraphBuilder() {
    }

    public static MolecularGraph build(Molecule ast) {
        GraphBuilder builder = new GraphBuilder();
        ast.accept(builder);
        return new MolecularGraph(builder.atoms, builder.bonds);
    }

    @Override
    public Integer visitMolecule(Molecule node) {
        return node.chain().accept(this);
    }

    @Override
    public Integer visitChain(Chain node) {
        int first = node.first().accept(this);
        int previous = first;
        for (Link link : node.links()) {
            origin = previous;
            previous = link.accept(this);
        }
        return first;
    }

    @Override
    public Integer visitLink(Link node) {
        // Guardamos origin en local porque las ramas anidadas lo sobrescriben
        int from = origin;
        int to = addBondToNextAtom(from, node.bond().order());
        node.atom().accept(this);
        return to;
    }

    @Override
    public Integer visitBranchedAtom(BranchedAtom node) {
        int atom = node.atom().accept(this);
        for (Branch branch : node.branches()) {
            origin = atom;
            branch.accept(this);
        }
        return atom;
    }

    @Override
    public Integer visitBranch(Branch node) {
        int from = origin;
        int to = addBondToNextAtom(from, node.bond().order());
        node.chain().accept(this);
        return to;
    }

    @Override
    public Integer visitBond(Bond node) {
        // Un Bond suelto no sabe qué átomos une, así que se maneja en Link/Branch
        throw new UnsupportedOperationException("los enlaces se procesan en visitLink y visitBranch");
    }

    @Override
    public Integer visitAtom(Atom node) {
        int index = atoms.size();
        atoms.add(new MolecularGraph.Atom(index, node.element(), node.position()));
        return index;
    }

    // Creamos el enlace antes de visitar el subárbol para mantener el orden del SMILES;
    // el primer átomo que se cree dentro tendrá justo el índice atoms.size().
    private int addBondToNextAtom(int from, BondOrder order) {
        int to = atoms.size();
        bonds.add(new MolecularGraph.Bond(from, to, order));
        return to;
    }
}