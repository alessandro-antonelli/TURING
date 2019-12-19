package turing.exceptions;

public class NoSuchUserException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7872766950256736078L;

	public NoSuchUserException(String s)
	{
		super(s);
	}

	public NoSuchUserException()
	{
		super("Errore: impossibile eseguire, l'utente specificato non esiste");
	}
}
