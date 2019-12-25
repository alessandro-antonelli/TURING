package turing.exceptions;

public class NonPositiveNumberException extends EccezioneTuring {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1340781358762434720L;

	public NonPositiveNumberException(String s)
	{
		super(s);
	}
	
	public NonPositiveNumberException()
	{
		super("Errore: impossibile eseguire, il numero deve essere positivo");
	}
}
