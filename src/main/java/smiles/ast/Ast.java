package smiles.ast;

import java.util.List;
import java.util.Objects;

/**
 * AST para el subconjunto de SMILES.
 * Los enlaces implícitos se guardan como nodos Bond simples para evitar nulls.
 */
public final class Ast {

    private Ast() {
    }

    public sealed interface Node permits Molecule, Chain, Link, BranchedAtom, Branch, Bond, Atom {
        <R> R accept(Visitor<R> visitor);
    }

    public record Molecule(Chain chain) implements Node {
        public Molecule {
            Objects.requireNonNull(chain, "chain");
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitMolecule(this);
        }
    }

    // Cadena principal o de una rama: átomo inicial y los enlaces siguientes
    public record Chain(BranchedAtom first, List<Link> links) implements Node {
        public Chain {
            Objects.requireNonNull(first, "first");
            links = List.copyOf(links);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitChain(this);
        }
    }

    public record Link(Bond bond, BranchedAtom atom) implements Node {
        public Link {
            Objects.requireNonNull(bond, "bond");
            Objects.requireNonNull(atom, "atom");
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLink(this);
        }
    }

    public record BranchedAtom(Atom atom, List<Branch> branches) implements Node {
        public BranchedAtom {
            Objects.requireNonNull(atom, "atom");
            branches = List.copyOf(branches);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBranchedAtom(this);
        }
    }

    public record Branch(Bond bond, Chain chain) implements Node {
        public Branch {
            Objects.requireNonNull(bond, "bond");
            Objects.requireNonNull(chain, "chain");
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBranch(this);
        }
    }

    public enum BondOrder {
        SINGLE(1), DOUBLE(2);

        private final int value;

        BondOrder(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    // explicit indica si tenía '-' o '=' en la entrada; position es la columna (desde 0)
    public record Bond(BondOrder order, boolean explicit, int position) implements Node {
        public Bond {
            Objects.requireNonNull(order, "order");
        }

        public static Bond implicitSingle(int position) {
            return new Bond(BondOrder.SINGLE, false, position);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBond(this);
        }
    }

    public record Atom(String element, int position) implements Node {
        public Atom {
            Objects.requireNonNull(element, "element");
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAtom(this);
        }
    }

    public interface Visitor<R> {
        R visitMolecule(Molecule node);

        R visitChain(Chain node);

        R visitLink(Link node);

        R visitBranchedAtom(BranchedAtom node);

        R visitBranch(Branch node);

        R visitBond(Bond node);

        R visitAtom(Atom node);
    }
}
