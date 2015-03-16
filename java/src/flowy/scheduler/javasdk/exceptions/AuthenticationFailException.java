package flowy.scheduler.javasdk.exceptions;

public class AuthenticationFailException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3397101375217416532L;

	public AuthenticationFailException() {
		super("Authentication failed");
	}
}
