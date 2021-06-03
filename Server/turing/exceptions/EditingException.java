package turing.exceptions;

public class EditingException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2797311098490105732L;

	public EditingException(String s)
	{
		super(s);
	}

	public EditingException()
	{
		super("Errore: impossibile eseguire mentre stai modificando un documento!");
	}
}
