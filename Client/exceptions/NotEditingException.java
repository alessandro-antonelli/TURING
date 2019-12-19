package turing.exceptions;

public class NotEditingException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5375441587528411737L;

	public NotEditingException(String s)
	{
		super(s);
	}

	public NotEditingException()
	{
		super("Errore: per eseguirlo devi stare eseguendo la modifica di un documento");
	}
}
