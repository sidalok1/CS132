# CS132
## Homeworks/Projects for CS 132

CS 132, Compiler Construction, taught by professor Palsberg during Spring quarter 2025 at UCLA.

The ultimate goal for the project is to build a MiniJava to RISC-V compiler. Sparrow/Sparrow-V is used as and 
intermediate representation. All work is done in Java.
While still relevant, [Homework 1](#homework-1) does not directly contribute to the project, which is covered by 
homeworks [2](), [3](), [4](), and [5]().

---

### Homework 1
#### Implement a Recursive Descent Parser

Using the principles taught on [LL(1) Academy](http://ll1academy.cs.ucla.edu/), a parser for a subset of MiniJava is 
designed. The parser can be called from the following command:

```shell
  java Parse < program
```

Specifically, the parser is for the following grammar:
```
S ::= { L } | System.out.println ( E ) ; | if ( E ) S else S | while ( E ) S
L ::= S L | ϵ
E ::= true | false | ! E
```