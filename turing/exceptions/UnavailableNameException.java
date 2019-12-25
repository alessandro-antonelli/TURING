package turing.exceptions;

public class UnavailableNameException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9069967602463097164L;

	public UnavailableNameException(String s)
	{
		super(s);
	}
	
	public UnavailableNameException()
	{
		super("Errore: il nome che hai indicato non è disponibile (già occupato)!");
	}
}
