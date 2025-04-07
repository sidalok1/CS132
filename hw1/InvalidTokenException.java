public class InvalidTokenException extends RuntimeException {
	public final char c;
	public final int char_num;
	public InvalidTokenException(char c, int char_num) {
		super(String.format("Invalid character: %c at position %d", c, char_num));
		this.c = c;
		this.char_num = char_num;
	}
}
