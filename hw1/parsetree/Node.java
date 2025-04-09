package parsetree;
import java.util.ArrayList;
import java.util.List;

/**
 * Data structure representing a Node is the parse tree. The head is the symbol
 * which is a Terminal, Nonterminal or Epsilon, and the children (stored in the
 * production field, a list) representing the tokens making up the parsed
 * production rule.
 * @see Symbol
 */
public class Node {
	public final Symbol symbol;
	public final List<Node> production;
	public Node(Symbol symbol) {
		this.symbol = symbol;
		this.production = new ArrayList<>();
	}
}
