import parsetree.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Queue;

public class Parser {

	public static final EnumMap<Nonterminal, EnumMap<Terminal, Symbol[]>> parseTable;

	static {
		/*
		 * Outer EnumMap maps Nonterminal to inner EnumMap. Inner EnumMap maps
		 * Token to the production rule that should be selected.
		 */
		parseTable = new EnumMap<>(Nonterminal.class);

		/*
		 * Production rules for S
		 */
		EnumMap<Terminal, Symbol[]> s = new EnumMap<>(Terminal.class);
		Symbol[] s_one = {Terminal.LBRACKET, Nonterminal.L, Terminal.RBRACKET};
		Symbol[] s_two = {Terminal.SYS_OUT_PRINTLN, Terminal.LPAREN,
				Nonterminal.E, Terminal.RPAREN, Terminal.SEMICOLON};
		Symbol[] s_three = {Terminal.IF, Terminal.LPAREN, Nonterminal.E,
				Terminal.RPAREN, Nonterminal.S, Terminal.ELSE, Nonterminal.S};
		Symbol[] s_four = {Terminal.WHILE, Terminal.LPAREN, Nonterminal.E,
							Terminal.RPAREN, Nonterminal.S};
		s.put((Terminal) s_one[0], s_one);
		s.put((Terminal) s_two[0], s_two);
		s.put((Terminal) s_three[0], s_three);
		s.put((Terminal) s_four[0], s_four);
		parseTable.put(Nonterminal.S, s);

		/*
		 * Production rules for L
		 */
		EnumMap<Terminal, Symbol[]> l = new EnumMap<>(Terminal.class);
		Symbol[] l_one = {Nonterminal.S, Nonterminal.L};
		Symbol[] l_two = {Epsilon.INSTANCE};
		l.put(Terminal.LBRACKET, l_one);
		l.put(Terminal.RBRACKET, l_two);
		l.put(Terminal.SYS_OUT_PRINTLN, l_one);
		l.put(Terminal.IF, l_one);
		l.put(Terminal.WHILE, l_one);
		parseTable.put(Nonterminal.L, l);

		/*
		 * Production rules for E
		 */
		EnumMap<Terminal, Symbol[]> e = new EnumMap<>(Terminal.class);
		Symbol[] e_one = {Terminal.TRUE};
		Symbol[] e_two = {Terminal.FALSE};
		Symbol[] e_three = {Terminal.EXCLAMATION, Nonterminal.E};
		e.put((Terminal) e_one[0], e_one);
		e.put((Terminal) e_two[0], e_two);
		e.put((Terminal) e_three[0], e_three);
		parseTable.put(Nonterminal.E, e);
	}

	private final Node root;
	private final Queue<Terminal> tokens;

	public Parser(Queue<Terminal> tokens) throws ParseFailed {
		root = new Node(Nonterminal.S);
		this.tokens = tokens;
		this.parse(root);
	}
	private void parse(Node node) throws ParseFailed {
		Terminal nextToken = this.tokens.peek();
		Nonterminal nonterminal = (Nonterminal) node.symbol;
		if (nextToken == null) {
			throw new ParseFailed("Failed to parse tree, ran out of tokens");
		}
		Symbol[] production_rule = parseTable.get(nonterminal).get(nextToken);
		if (production_rule == null) {
			throw new ParseFailed(
					"Failed to parse tree, no production rule found expanding "+
					nonterminal.name() + " to a rule starting with "+
					nextToken.name());
		}
		Node next;
		for (Symbol symbol : production_rule) {
			String symbolType = symbol.getClass().getSimpleName();
			switch (symbolType) {
				case "Epsilon" :
					break;
				case "Nonterminal" :
					next = new Node(symbol);
					this.parse(next);
					node.production.add(next);
					break;
				case "Terminal" :
					Terminal next_terminal = this.tokens.poll();
					if (symbol == next_terminal) {
						next = new Node(next_terminal);
						node.production.add(next);
					} else if (next_terminal == null) {
						throw new ParseFailed("Failed to parse tree, ran out of tokens");
					} else {
						throw new ParseFailed("Failed to parse tree, invalid token: " +
								next_terminal.name());
					}
					break;
			}
		}
	}
}
