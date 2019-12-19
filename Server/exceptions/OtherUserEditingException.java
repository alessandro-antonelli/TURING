package turing.exceptions;

public class OtherUserEditingException extends EccezioneTuring {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8007587224247133038L;

	public OtherUserEditingException(String s)
	{
		super(s);
	}

	public OtherUserEditingException()
	{
		super("Errore: impossibile eseguire mentre c'è già un altro utente che sta modificando la sezione!");
	}

}
