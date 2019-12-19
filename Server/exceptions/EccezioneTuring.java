package turing.exceptions;

public abstract class EccezioneTuring extends Exception {

	private static final long serialVersionUID = 2110897038929941230L;
	
	public EccezioneTuring(String s)
	{
		super(s);
	}

	public EccezioneTuring()
	{
		super();
	}
	
}
