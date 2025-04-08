package parsetree;
import java.util.LinkedList;

public class Node {
	public final Symbol symbol;
	public final LinkedList<Node> production;
	public Node(Symbol symbol) {
		this.symbol = symbol;
		this.production = new LinkedList<>();
	}
}
