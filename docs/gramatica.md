# Gramática del subconjunto SMILES

## 1. Alfabeto (terminales)

| Terminal | Lexema | Significado |
|----------|--------|-------------|
| `ATOM`   | `C`, `O`, `N` | Átomo alifático de carbono, oxígeno o nitrógeno |
| `BOND`   | `-`, `=` | Enlace simple explícito o enlace doble |
| `LPAREN` | `(` | Inicio de rama |
| `RPAREN` | `)` | Fin de rama |
| `EOF`    | (fin de la cadena) | Fin de la entrada |

Si entre dos átomos no hay símbolo de enlace, el enlace es simple (enlace implícito).
Por eso `CC` y `C-C` representan la misma molécula.

## 2. Gramática en EBNF

```
molecule     ::= chain EOF
chain        ::= branchedAtom ( bond? branchedAtom )*
branchedAtom ::= atom branch*
branch       ::= '(' bond? chain ')'
bond         ::= '-' | '='
atom         ::= 'C' | 'O' | 'N'
```

Está basada en la gramática de OpenSMILES, sin anillos, aromáticos, átomos entre
corchetes, cargas ni estereoquímica.

## 3. Gramática en BNF (sin operadores `*` ni `?`)

Para calcular FIRST y FOLLOW se reescriben las repeticiones y los opcionales como
reglas con ε:

```
molecule     → chain EOF
chain        → branchedAtom chainTail
chainTail    → bondOpt branchedAtom chainTail | ε
bondOpt      → bond | ε
branchedAtom → atom branches
branches     → branch branches | ε
branch       → '(' bondOpt chain ')'
bond         → '-' | '='
atom         → 'C' | 'O' | 'N'
```

La gramática no tiene recursión izquierda: `chainTail` y `branches` son recursivos
por la derecha.

## 4. Conjuntos FIRST

| No terminal | FIRST |
|-------------|-------|
| atom         | { C, O, N } |
| bond         | { -, = } |
| bondOpt      | { -, =, ε } |
| branch       | { ( } |
| branches     | { (, ε } |
| branchedAtom | { C, O, N } |
| chainTail    | { -, =, C, O, N, ε } |
| chain        | { C, O, N } |
| molecule     | { C, O, N } |

## 5. Conjuntos FOLLOW

| No terminal | FOLLOW |
|-------------|--------|
| molecule     | { $ } |
| chain        | { EOF, ) } |
| chainTail    | { EOF, ) } |
| branchedAtom | { -, =, C, O, N, EOF, ) } |
| branches     | { -, =, C, O, N, EOF, ) } |
| branch       | { (, -, =, C, O, N, EOF, ) } |
| bondOpt      | { C, O, N } |
| bond         | { C, O, N } |
| atom         | { (, -, =, C, O, N, EOF, ) } |

## 6. Verificación LL(1)

Una gramática es LL(1) si, para cada no terminal con varias alternativas, el
parser puede elegir la alternativa mirando un solo token. Para cada par de
alternativas `A → α | β`:

1. FIRST(α) ∩ FIRST(β) = ∅
2. Si β puede derivar ε, entonces FIRST(α) ∩ FOLLOW(A) = ∅

| No terminal | Alternativa 1 | Alternativa ε | Intersección |
|-------------|---------------|---------------|--------------|
| chainTail | FIRST = { -, =, C, O, N } | FOLLOW = { EOF, ) } | ∅ |
| bondOpt   | FIRST = { -, = } | FOLLOW = { C, O, N } | ∅ |
| branches  | FIRST = { ( } | FOLLOW = { -, =, C, O, N, EOF, ) } | ∅ |

`bond` y `atom` tienen alternativas que empiezan con terminales distintos.
Por lo tanto, la gramática es LL(1) y se puede implementar con un parser
descendente recursivo sin retroceso.

## 7. Tabla de análisis LL(1)

Cada celda indica qué producción aplicar según el no terminal (fila) y el token
siguiente (columna). Una celda vacía es un error sintáctico.

| | C / O / N | - / = | ( | ) | EOF |
|---|---|---|---|---|---|
| molecule     | chain EOF | | | | |
| chain        | branchedAtom chainTail | | | | |
| chainTail    | bondOpt branchedAtom chainTail | bondOpt branchedAtom chainTail | | ε | ε |
| bondOpt      | ε | bond | | | |
| branchedAtom | atom branches | | | | |
| branches     | ε | ε | branch branches | ε | ε |
| branch       | | | ( bondOpt chain ) | | |
| bond         | | `-` o `=` | | | |
| atom         | `C`, `O` o `N` | | | | |

## 8. Ejemplo de derivación: acetona `CC(=O)C`

Derivación por la izquierda (resumida):

```
molecule
⇒ chain EOF
⇒ branchedAtom chainTail EOF
⇒ C branches chainTail EOF                          (branches → ε)
⇒ C bondOpt branchedAtom chainTail EOF              (bondOpt → ε)
⇒ C C branches chainTail EOF
⇒ C C branch branches chainTail EOF
⇒ C C ( bondOpt chain ) branches chainTail EOF
⇒ C C ( = chain ) branches chainTail EOF
⇒ C C ( = O ) branches chainTail EOF                (branches → ε)
⇒ C C ( = O ) bondOpt branchedAtom chainTail EOF
⇒ C C ( = O ) C chainTail EOF                       (chainTail → ε)
⇒ C C ( = O ) C EOF
```

## 9. Qué valida la sintaxis y qué no

| Entrada | Resultado | Fase que lo detecta |
|---------|-----------|---------------------|
| `CCX`   | Error: carácter no reconocido `X` | Léxica |
| `c1ccccc1` | Error: aromáticos y anillos fuera de alcance | Léxica |
| `CC(O`  | Error: falta `)` | Sintáctica |
| `CC=`   | Error: enlace sin átomo a la derecha | Sintáctica |
| `C()`   | Error: rama vacía | Sintáctica |
| `(C)C`  | Error: la molécula no puede empezar con rama | Sintáctica |
| (vacía) | Error: se esperaba un átomo | Sintáctica |
| `C=O=C` | Sintácticamente válida | Semántica (valencia del O) |
| `C(C)(C)(C)(C)C` | Sintácticamente válida | Semántica (valencia del C central = 5) |

La gramática describe la forma de la cadena. Las reglas químicas, como las valencias,
dependen del contexto y se verifican en el análisis semántico.

## Referencias

- OpenSMILES Specification. https://opensmiles.org/opensmiles
- Apodaca, R. (2020). SMILES Formal Grammar Revisited. Depth-First.
  https://depth-first.com/articles/2020/12/21/smiles-formal-grammar-revisited/
- Weininger, D. (1988). SMILES, a chemical language and information system. 1.
  *J. Chem. Inf. Comput. Sci.*, 28(1), 31–36.
- Aho, A. V., Lam, M. S., Sethi, R., & Ullman, J. D. (2006). *Compilers: Principles,
  Techniques, and Tools* (2.ª ed.), secciones 4.4.2 (FIRST y FOLLOW) y 4.4.3 (LL(1)).
