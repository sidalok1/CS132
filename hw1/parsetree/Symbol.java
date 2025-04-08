package parsetree;

public interface Symbol {}

/*
public class Symbol {
	private final String type;
	public final Terminal t;
	public final Nonterminal n;
	public final Epsilon e;
	public Symbol(String type, Terminal t, Nonterminal n, Epsilon e) {
		this.type = type;
		switch (type) {
			case "Terminal":
				this.t = t;
				this.n = null;
				this.e = null;
				break;
			case "Nonterminal":
				this.t = null;
				this.n = n;
				this.e = null;
				break;
			case "Epsilon":
				this.t = null;
				this.n = null;
				this.e = e;
				break;
			default:
				this.t = null;
				this.n = null;
				this.e = null;
				break;
		}
	}

	public String getType() {
		return type;
	}
}*/
