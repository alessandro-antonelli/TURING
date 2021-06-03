package turing.exceptions;

public class AlreadyLoggedException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2155961927368085458L;

	public AlreadyLoggedException(String s)
	{
		super(s);
	}

	public AlreadyLoggedException()
	{
		super("Errore: impossibile eseguire mentre sei già loggato in un account!");
	}
}
