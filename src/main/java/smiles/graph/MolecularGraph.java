package smiles.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import smiles.ast.Ast.BondOrder;

/**
 * Grafo molecular no dirigido que representa la conectividad de la molécula
 * (átomos como nodos y enlaces como aristas). Se construye desde GraphBuilder y
 * sobre él trabajan el análisis semántico, los grupos funcionales y Joback.
 */
public final class MolecularGraph {

    // position es la columna (base 0) en el SMILES original, útil para mensajes de error
    public record Atom(int index, String element, int position) {
        public Atom {
            Objects.requireNonNull(element, "element");
        }
    }

    public record Bond(int from, int to, BondOrder order) {
        public Bond {
            Objects.requireNonNull(order, "order");
        }

        public int other(int atom) {
            if (atom == from) {
                return to;
            }
            if (atom == to) {
                return from;
            }
            throw new IllegalArgumentException("el átomo " + atom + " no pertenece a este enlace");
        }
    }

    private final List<Atom> atoms;
    private final List<Bond> bonds;
    private final List<List<Bond>> adjacency;

    MolecularGraph(List<Atom> atoms, List<Bond> bonds) {
        this.atoms = List.copyOf(atoms);
        this.bonds = List.copyOf(bonds);
        List<List<Bond>> adj = new ArrayList<>();
        for (int i = 0; i < this.atoms.size(); i++) {
            adj.add(new ArrayList<>());
        }
        for (Bond b : this.bonds) {
            adj.get(b.from()).add(b);
            adj.get(b.to()).add(b);
        }
        List<List<Bond>> frozen = new ArrayList<>();
        for (List<Bond> list : adj) {
            frozen.add(Collections.unmodifiableList(list));
        }
        this.adjacency = Collections.unmodifiableList(frozen);
    }

    public List<Atom> atoms() {
        return atoms;
    }

    public List<Bond> bonds() {
        return bonds;
    }

    public Atom atom(int index) {
        return atoms.get(index);
    }

    public List<Bond> bondsOf(int atom) {
        return adjacency.get(atom);
    }

    public List<Integer> neighbors(int atom) {
        List<Integer> result = new ArrayList<>();
        for (Bond b : adjacency.get(atom)) {
            result.add(b.other(atom));
        }
        return result;
    }

    // Vecinos explícitos (sin contar hidrógenos implícitos)
    public int degree(int atom) {
        return adjacency.get(atom).size();
    }

    // Suma de órdenes de enlace (simple = 1, doble = 2) para calcular valencia usada e H implícitos
    public int bondOrderSum(int atom) {
        int sum = 0;
        for (Bond b : adjacency.get(atom)) {
            sum += b.order().value();
        }
        return sum;
    }
}