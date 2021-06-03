package turing.exceptions;

public class WrongPasswordException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3232447293040615580L;

	public WrongPasswordException(String s)
	{
		super(s);
	}

	public WrongPasswordException()
	{
		super("Errore: la password che hai indicato è sbagliata!");
	}
}
