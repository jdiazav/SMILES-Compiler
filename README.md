# Compilador SMILES – Estimación de temperatura de ebullición (Joback)

Proyecto final de Compiladores 2026-2.

Compilador de un subconjunto del lenguaje SMILES. Lee una cadena SMILES, la analiza
(léxico y sintaxis), construye un árbol de sintaxis abstracta (AST), lo transforma en
un grafo molecular, valida la estructura y estima la temperatura de ebullición con el
método de contribución de grupos de Joback:

```
Tb = 198.2 + Σ Ni · Cb,i   [K]
```

## Subconjunto soportado

- Átomos alifáticos: `C`, `O`, `N`
- Enlaces simples (`-` o implícito) y dobles (`=`)
- Ramificaciones con paréntesis: `CC(=O)C`

La gramática está en [`docs/gramatica.md`](docs/gramatica.md).

## Requisitos

- JDK 17 o superior
- Maven 3.8 o superior

## Compilar

```bash
mvn package
```

## Ejecutar

```bash
java -jar target/smiles-compiler.jar "CC(=O)C"
```

Fases implementadas por ahora:

- Análisis léxico: muestra la tabla de tokens o el error léxico con la columna señalada.
- Análisis sintáctico: muestra el AST como árbol o el error sintáctico con la columna
  señalada.
- Grafo molecular: transforma el AST en un grafo (átomos = nodos, enlaces = aristas)
  y muestra la tabla de átomos y la de enlaces.

Ejemplo de AST para `CC(=O)C`:

```
Molecule
└── Chain
    ├── BranchedAtom
    │   └── Atom C  [col 1]
    ├── Link
    │   ├── Bond SINGLE (implícito)
    │   └── BranchedAtom
    │       ├── Atom C  [col 2]
    │       └── Branch
    │           ├── Bond DOUBLE '='  [col 4]
    │           └── Chain
    │               └── BranchedAtom
    │                   └── Atom O  [col 5]
    └── Link
        ├── Bond SINGLE (implícito)
        └── BranchedAtom
            └── Atom C  [col 7]
```

Grafo molecular para `CC(=O)C`:

```
4 átomos, 3 enlaces

Átomos (nodos):
Átomo  Elemento  Columna  Grado  Σ órdenes   Vecinos
C1     C         1        1      1           C2
C2     C         2        3      4           C1, O3, C4
O3     O         5        1      2           C2
C4     C         7        1      1           C2

Enlaces (aristas):
#    Orden    Átomos
1    SINGLE   C1 - C2
2    DOUBLE   C2 = O3
3    SINGLE   C2 - C4
```

`Grado` es el número de átomos vecinos y `Σ órdenes` la suma de los órdenes de sus
enlaces (simple = 1, doble = 2). El grafo no valida valencias: `C=O=C` produce un
oxígeno con `Σ órdenes = 4`, que el análisis semántico debe rechazar.

### Uso del grafo en las fases siguientes

El análisis semántico, los grupos funcionales y Joback trabajan sobre
`smiles.graph.MolecularGraph`, no sobre el AST:

```java
MolecularGraph g = GraphBuilder.build(ast);
for (MolecularGraph.Atom a : g.atoms()) {
    g.neighbors(a.index());     // índices de los átomos vecinos
    g.degree(a.index());        // número de vecinos
    g.bondOrderSum(a.index());  // suma de órdenes de enlace
    g.bondsOf(a.index());       // enlaces del átomo (MolecularGraph.Bond)
}
```


## Equipo

- Juan Diego Diaz Avila 
- Laura Valentina Diaz Velandia
- Daniela Peñaranda Jaime