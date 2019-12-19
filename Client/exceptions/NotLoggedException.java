package turing.exceptions;

public class NotLoggedException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1773572517450164219L;

	public NotLoggedException(String s)
	{
		super(s);
	}

	public NotLoggedException()
	{
		super("Errore: per eseguirlo devi essere loggato in un account");
	}
}
