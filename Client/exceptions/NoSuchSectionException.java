package turing.exceptions;

public class NoSuchSectionException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3763837148371954640L;

	public NoSuchSectionException(String s)
	{
		super(s);
	}
	
	public NoSuchSectionException()
	{
		super("Errore: impossibile eseguire, la sezione specificata non esiste");
	}
}
