package turing.exceptions;

public class UserRightsException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8722077355305710690L;

	public UserRightsException(String s)
	{
		super(s);
	}

	public UserRightsException()
	{
		super("Errore: il tuo account utente non dispone dell'autorizzazione per eseguirlo!");
	}
}
