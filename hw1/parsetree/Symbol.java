package parsetree;

public sealed interface Symbol permits Terminal, Nonterminal, Epsilon {}