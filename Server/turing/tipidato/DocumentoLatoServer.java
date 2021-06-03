package turing.tipidato;

import java.net.InetAddress;

/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG
Codice sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

/*
 * La classe rappresenta un documento di TURING nel modo più completo possibile, memorizzando sia le informazioni
 * sul documento in sé, sia quelle di "book keeping" relative alla sua gestione lato server.
 * 
 * E' un'estensione del tipo DocumentoConDettagli, che rappresenta le sole informazioni sul documento in sé.
 */

public class DocumentoLatoServer extends DocumentoConDettagli {
	
	private static final long serialVersionUID = -3988015562341956067L;

	private long[] dimensioneSezione;
	
	private long dimensioneTotale;
	
	private String[] modificatoreDellaSezione;
	// La i-esima posizione dell'array indica il nome dell'utente che sta modificando la i-esima sezione del documento,
	// oppure NULL se nessuno la sta modificando
	
	private int numSezioniInCorsoDiModifica;
	// Indica su quante sezioni del documento c'è una modifica in corso
	
	private InetAddress indirizzoUDPChat;
	// Memorizza l'indirizzo UDP utilizzato per la chat tra i collaboratori del documento.
	// Vale NULL se la chat non è stata creata (perché non c'è nessuna sezione in corso di modifica).
	
	public final Object lockTuttiFile;
	/*
	 *  Oggetto di pura sincronizzazione. La sua lock implicita viene utilizzata per regolare l'accesso su disco ai file
	 *  che memorizzano le varie sezioni di cui è composto il documento.
	 *  La lock consente o vieta l'accesso ai file di TUTTE le sezioni del documento (è necessario acquisirla anche se si
	 *  deve accedere ad una sola di esse), ed è intesa come regolatrice sia dell'accesso in lettura che
	 *  dell'accesso in scrittura di tali file (quindi è necessario acquisire la lock sia che si debba modificare il file,
	 *  sia che si debba solo leggerlo).
	 */

	public DocumentoLatoServer(String nomeDocumento, String creatore, int numSezioni)
	{
		super(nomeDocumento, creatore, numSezioni);
		
		modificatoreDellaSezione = new String[numSezioni];
		dimensioneSezione = new long[numSezioni];
		for(int i=0; i<numSezioni; i++)
		{
			modificatoreDellaSezione[i] = null;
			dimensioneSezione[i] = 0;
		}
		
		dimensioneTotale = 0;
		
		numSezioniInCorsoDiModifica = 0;
		
		indirizzoUDPChat = null;
		
		lockTuttiFile = new Object();
	}
	
	//======= METODI GETTER =======
	
	synchronized public Boolean CiSonoSezioniInModifica()
	{
		return (numSezioniInCorsoDiModifica != 0);
	}
	
	synchronized public Boolean QualcunoStaModificandoLaSezione(int sezione)
	{
		if(sezione < 0 || sezione >= this.getNumSezioni()) return true;
		
		return (modificatoreDellaSezione[sezione] != null);
	}
	
	synchronized public String ChiStaModificandoLaSezione(int sezione)
	{
		if(sezione < 0 || sezione >= this.getNumSezioni()) return null;
		if(modificatoreDellaSezione[sezione] == null) return null;
		
		return new String (modificatoreDellaSezione[sezione]);
	}
	
	synchronized public Boolean HaPermessi(String username)
	{
		if(this.getCreatore().equals(username) || this.isCollaboratore(username)) return true;
		else return false;
	}
	
	synchronized public InetAddress GetIndirizzoChat()
	{
		if(numSezioniInCorsoDiModifica == 0) return null;
		
		return indirizzoUDPChat;
	}
	
	synchronized public long getDimensioneTotale()
	{
		return dimensioneTotale;
	}

	//======= METODI SETTER =======
	
	synchronized public void RegistraInizioModifica(String username, int sezione, InetAddress indirizzoUDPChat)
			throws RuntimeException
	{
		if(sezione < 0 || sezione >= this.getNumSezioni()) throw new IllegalArgumentException();
		if(username == null || username.isEmpty()) throw new IllegalArgumentException();

		if(numSezioniInCorsoDiModifica == 0) //E' il primo utente che modifica il documento
		{
			if(indirizzoUDPChat == null)
				throw new RuntimeException("E' il primo utente che modifica il documento: è necessario chiamare il metodo con due parametri" +
						"e specificare anche l'indirizzo della chat!");
			else
				this.indirizzoUDPChat = indirizzoUDPChat;
		}
		if(numSezioniInCorsoDiModifica > 0 && indirizzoUDPChat != null)
			throw new RuntimeException("Ci sono già altri utenti che modificano il documento, pertanto la chat è già stata creata: " +
					"hai invocato il metodo con due parametri specificando un ulteriore indirizzo per la chat, probabilmente hai sbagliato");
		
		modificatoreDellaSezione[sezione] = username;
		numSezioniInCorsoDiModifica++;
	}
	
	// Restituisce al chiamante un InetAddress che indica l'eventuale indirizzo UDP che, dopo la fine della modifica, si libera
	// e può essere rimesso all'interno del pool degli indirizzi utilizzabili per altre chat (nel caso in cui fosse l'unico rimasto ad editare).
	// Restituisce null se non si libera nessun indirizzo (perché altri utenti stanno editando e la chat deve rimanere in funzione).
	// Se invocata con newSize negativo, non modifica il conteggio delle dimensioni della sezione e del documento.
	synchronized public InetAddress RegistraFineModifica(int sezione, long newSize)
	{		
		modificatoreDellaSezione[sezione] = null;
		
		if(newSize >= 0)
		{
			dimensioneTotale = dimensioneTotale - dimensioneSezione[sezione] + newSize;
			dimensioneSezione[sezione] = newSize;
		}
		
		numSezioniInCorsoDiModifica--;
		if(numSezioniInCorsoDiModifica == 0)
		{
			InetAddress temp = this.indirizzoUDPChat;
			this.indirizzoUDPChat = null;
			return temp;
		}
		else
			return null;
	}
}
