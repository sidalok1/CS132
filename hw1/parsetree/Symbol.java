package parsetree;

/**
 * Generic type for a symbol. This program is written for compatibility
 * with Java 8, and would be implemented as a sealed interface that permits
 * Terminal, Nonterminal and Epsilon, if a newer version of Java were acceptable
 * @see Terminal
 * @see Nonterminal
 * @see Epsilon
 */
public interface Symbol {}