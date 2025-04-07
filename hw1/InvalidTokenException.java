public class InvalidTokenException extends RuntimeException {
	public final String token;
	public final int char_num;
	public InvalidTokenException(String token, int char_num) {
		super(String.format("Invalid token: %s at position %d",
											token,          char_num));
		this.token = token;
		this.char_num = char_num;
	}
}
