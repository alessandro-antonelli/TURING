package turing.exceptions;

public class NoSuchDocumentException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8690264914929961768L;

	public NoSuchDocumentException(String s)
	{
		super(s);
	}
	
	public NoSuchDocumentException()
	{
		super("Errore: impossibile eseguire, il documento specificato non esiste");
	}
}
