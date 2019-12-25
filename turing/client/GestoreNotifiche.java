/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Client
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.client;

import turing.tipidato.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

/*
 * 		GestoreNotifiche
 * Questa classe definisce la struttura utilizzata per gestire la ricezione di notifiche dal server,
 * con il codice eseguito dal thread "ascoltatore" e la coda delle notifiche pendenti acceduta dal thread principale.
 */

public class GestoreNotifiche implements Runnable {
	
	//======= VAR DI ISTANZA =======
	
	private final Vector<Documento> condivisioniPendenti;
	// Coda nella quale sono immagazzinate le notifiche ricevute dal server ma non ancora visualizzate dall'utente.
	// Viene scritta dal thread gestore delle notifiche (metodo run);
	// viene letta e poi svuotata dal thread gestore dell'interfaccia grafica (metodo getNotifichePendenti).
	// La sincronizzazione è realizzata tramite l'uso di blocchi synchronized(condivisioniPendenti)
	
	private Boolean eseguitoPrimoControllo;
	// Varibile che indica se la prima ricezione di dati dal server è stata già effettuata oppure no.
	// Viene acceduta solo dopo aver acquisito la lock implicita della coda condivisioniPendenti.
	
	private final Socket sockNotifiche;
	// Socket TCP per la ricezione delle notifiche dal server
	
	private final ObjectInputStream stream;
	// Stream per la ricezione delle notifiche dal server tramite il socket TCP della connessione
	
	//======= COSTRUTTORE =======
	public GestoreNotifiche(Socket sockNotifiche) throws IOException
	{
		condivisioniPendenti = new Vector<Documento>();
		eseguitoPrimoControllo = false;
		this.sockNotifiche = sockNotifiche;
		this.stream = new ObjectInputStream(sockNotifiche.getInputStream());
	}

	/* ======= CODICE THREAD =======
	 * Metodo eseguito dal thread demone dedicato alla gestione delle notifiche.
	 * 
	 * Il thread rimane per tutta la durata in ascolto sul socket TCP delle notifiche, ad aspettare che arrivino condivisioni.
	 * Quando ne riceve, le salva nell'array condivisioniPendenti condiviso con il thread principale (gestore dell'interfaccia utente),
	 * il quale periodicamente stampa a schermo le notifiche e poi svuota l'array.
	 */
	@Override
	public void run()
	{
		try
		{
			while(!Thread.interrupted())
			{
				@SuppressWarnings("unchecked")
				Vector<Documento> insieme = (Vector<Documento>) stream.readObject();
				synchronized (condivisioniPendenti)
				{
					// Aggiungo le notifiche ricevute alla coda (a meno che non sia il documento vuoto di "inizio trasmissioni")
					for(Documento doc : insieme)
						if(doc != null && !doc.getCreatore().isEmpty())
							condivisioniPendenti.add(doc);
					
					// Alla prima iterazione, la ricezione della prima notifica avviene subito (se non ce ne sono, viene inviato
					// un documento vuoto di "inizio trasmissioni"); dopo la ricezione, imposto il flag "eseguitoPrimoControllo"
					// e segnalo al thread dell'interfaccia grafica che può procedere nell'invocazione del metodo getNotifichePendenti()
					if(eseguitoPrimoControllo == false)
					{
						eseguitoPrimoControllo = true;
						condivisioniPendenti.notify();
					}
				}
			}
			
			stream.close();
			if(!sockNotifiche.isInputShutdown()) sockNotifiche.shutdownInput();
			if(!sockNotifiche.isClosed()) sockNotifiche.close();
		}
		catch (ClassNotFoundException e) { e.printStackTrace(); }
		catch (SocketException | EOFException e) { return; } // Il client si è disconnesso
		catch (IOException e) { e.printStackTrace(); }
	}
	
	//======= METODI GETTER =======
	
	/*
	 * Metodo chiamato ed eseguito dal thread main (quello che gestisce l'interfaccia grafica).
	 */
	public Vector<Documento> getNotifichePendenti()
	{
		Vector<Documento> retval;
		synchronized(condivisioniPendenti)
		{
			while(eseguitoPrimoControllo == false)
				try { condivisioniPendenti.wait(); }
				catch (InterruptedException e) { }
			
			retval = new Vector<Documento>(condivisioniPendenti);
			condivisioniPendenti.clear();
		}
		return retval;
	}
}
