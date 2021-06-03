/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import turing.tipidato.DocumentoLatoServer;
import turing.tipidato.UtenteLatoServer;

/* 
 *		TuringServer
 * Questa classe Definisce l'oggetto "server del servizio TURING", con il suo
 * apparato di servizi, thread di supporto, di cartelle del file system e
 * strutture dati di book-keeping. Contiene il codice per inizializzare il
 * server e i metodi getter e setter per operarvi e per terminare la sua
 * esecuzione.
 */

public class TuringServer {
	
	//======= COSTANTI =======
	
	public final ConfigurazioneServer config;	
	
	//======= VAR DI ISTANZA =======

	private Set<UtenteLatoServer> utentiEsistenti;
	// Insieme degli utenti registrati nel sistema TURNG.
	// Acceduto dai vari thread del pool e sporadicamente dal thread principale (interfaccia grafica).
	// La sincronizzazione è ottenuta per mezzo dell'uso di blocchi synchronized(utentiEsistenti).
	
	private Set<DocumentoLatoServer> documentiEsistenti;
	// Insieme dei documenti memorizzati nel sistema TURING
	// Acceduto dai vari thread del pool e sporadicamente dal thread principale (interfaccia grafica).
	// La sincronizzazione è ottenuta per mezzo dell'uso di blocchi synchronized(documentiEsistenti).
	
	private Set<InetAddress> indirizziUDPinUso;
	// Insieme delle room UDP create e attive (con almeno un partecipante, ossia almeno un utente in fase di modifica del documento)
	// Acceduto dai vari thread del pool e sporadicamente dal thread principale (interfaccia grafica).
	// La sincronizzazione è ottenuta per mezzo dell'uso di blocchi synchronized(indirizziUDPinUso).

	private final ServerSocket welcomeSock;
	// Listening socket dove arrivano le richieste di connessione
	
	public final ThreadPoolExecutor pool;
	
	private final Thread threadAccett;
	
	private final CreatoreAccount gestoreRegistrazioni;
	
	//======= COSTRUTTORE =======
	
	public TuringServer(ConfigurazioneServer configurazione) throws IOException
	{
		this.utentiEsistenti = new HashSet<UtenteLatoServer>();
		this.documentiEsistenti = new HashSet<DocumentoLatoServer>();
		this.indirizziUDPinUso = new HashSet<InetAddress>();
		this.config = configurazione;
		
		// Inizializzo il database documenti sul file system
		try {
			File databaseFolder = new File(config.FILEBASEPATH);
			File parent = new File(databaseFolder.getParent());
			if(!parent.canWrite()) throw new IOException("Il programma non dispone dei diritti per scrivere sulla cartella del server (" + parent.getPath() + ")!");

			//Cancello eventuali contenuti della cartella (residui di esecuzioni precedenti terminate in modo anomalo)
			CancellaCartella(databaseFolder);
			//Ricreo la sola cartella radice
			if(databaseFolder.mkdir() == false) throw new IOException("Impossibile creare la cartella " + config.FILEBASEPATH);
		}
		catch (IOException e) {
			throw new IOException("Non è stato possibile inizializzare il database documenti nel file system", e);
		}
		
		// Creo il pool
		pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		
		// Creo il socket e il thread per ascoltare le richieste di connessione
		try {
			welcomeSock = new ServerSocket(config.PORTAPRINCIPALE, config.CodaWelcomeSock);
			
			TaskAccettaConnessioni taskAccett = new TaskAccettaConnessioni(this, config, welcomeSock);
			threadAccett = new Thread(taskAccett);
			threadAccett.setDaemon(true);
			threadAccett.start();
		}
		catch (java.net.BindException e)
			{ throw new IOException("Non è stato possibile creare il welcome socket per ricevere le connessioni in entrata, perché la porta " + config.PORTAPRINCIPALE + " è già in uso da parte di un'altra applicazione.\nControlla che non ci siano altre istanze del server in esecuzione, oppure usa gli argomenti a linea di comando per scegliere manualmente una porta diversa, poi riprova.", e); }
		catch (IOException e)
			{ throw new IOException("Non è stato possibile inizializzare il socket e il ", e); }
		
		// Creo il gestore delle richieste di registrazione account
		try { gestoreRegistrazioni = new GestoreRegistrazioniUtenti(this, configurazione); }
		catch (RemoteException e) {
			throw new RemoteException("Non è stato possibile inizializzare la comunicazione RMI, richiesta per il servizio di creazione account", e);
		}
	}
	
	//======= METODI GETTER =======
	
	public Boolean EsisteUtente(String username)
	{
		synchronized (utentiEsistenti)
		{
			for(UtenteLatoServer user : utentiEsistenti)
			{
				if(user.getNome().equals(username)) return true;
			}
			return false;
		}
	}
	
	public UtenteLatoServer GetUtente(String username)
	{
		synchronized (utentiEsistenti)
		{
			for(UtenteLatoServer user : utentiEsistenti)
			{
				if(user.getNome().equals(username)) return user;
			}
			return null;
		}
	}
	
	public Boolean EsisteDocumento(String docname)
	{
		synchronized(documentiEsistenti)
		{
			for(DocumentoLatoServer doc : documentiEsistenti)
			{
				if(doc.getNome().equals(docname)) return true;
			}
			return false;
		}
	}
	
	public DocumentoLatoServer GetDocumento(String docname)
	{
		synchronized(documentiEsistenti)
		{
			for(DocumentoLatoServer doc : documentiEsistenti)
			{
				if(doc.getNome().equals(docname)) return doc;
			}
			return null;
		}
	}
	
	public int getNumeroChatroom()
	{
		synchronized(indirizziUDPinUso)
		{ return indirizziUDPinUso.size(); }
	}
	
	public long getDimensioneTotale()
	{
		synchronized(documentiEsistenti)
		{
			long retVal = 0;
			for(DocumentoLatoServer doc : documentiEsistenti)
				retVal += doc.getDimensioneTotale();
			return retVal;
		}
	}
	
	public int getNumDocumentiEsistenti()
	{
		synchronized(documentiEsistenti)
		{ return documentiEsistenti.size(); }
	}
	
	//Restituisce una tripla di interi che indica quanti utenti sono offline, online e in modalità modifica
	public int[] getStatoUtenti()
	{
		int[] retVal = new int[3];
		retVal[0] = 0; retVal[1] = 0; retVal[2] = 0;
	
		synchronized(utentiEsistenti)
		{
			for(UtenteLatoServer user : utentiEsistenti)
			{
				if(user.getStato() == UtenteLatoServer.StatiUtente.Offline) retVal[0]++;
				else if(user.getStato() == UtenteLatoServer.StatiUtente.Online) retVal[1]++;
				else retVal[2]++;
			}
		}

		return retVal;
	}
	
	//======= METODI SETTER =======
	
	public void aggiungiUtente(UtenteLatoServer utente)
	{
		synchronized(utentiEsistenti)
		{ utentiEsistenti.add(utente); }
	}
	
	public void aggiungiDocumento(DocumentoLatoServer doc)
	{
		synchronized(documentiEsistenti)
		{ documentiEsistenti.add(doc); }
	}
	
	// Restituisce un indirizzo IP versione 4 di tipo multicast (intervallo 224-239.x.x.x)
	// generato casualmente, con la garanzia che non sia già usato da altre chatroom di Turing.
	// La procedura provvede inoltre a registrare l'indirizzo tra quelli già occupati.
	public InetAddress GeneraChatroom()
	{
		Random rand = new Random();
		InetAddress indGenerato = null;
		int primoOttetto, secondoOttetto, terzoOttetto, quartoOttetto;
		
		synchronized(indirizziUDPinUso)
		{
			do{
				primoOttetto = 224 + rand.nextInt(15);
				secondoOttetto = rand.nextInt(255);
				terzoOttetto = rand.nextInt(255);
				quartoOttetto = rand.nextInt(255);
				
				try {
					indGenerato = InetAddress.getByName(Integer.toString(primoOttetto) + '.' + Integer.toString(secondoOttetto) + '.' +
							Integer.toString(terzoOttetto) + '.' + Integer.toString(quartoOttetto));
				} catch (UnknownHostException e) { e.printStackTrace(); }
			} while(indirizziUDPinUso.contains(indGenerato) || IsReserved(primoOttetto, secondoOttetto, terzoOttetto, quartoOttetto));
			
			indirizziUDPinUso.add(indGenerato);
		}
		
		return indGenerato;
	}
	
	private Boolean IsReserved(int primoOttetto, int secondoOttetto, int terzoOttetto, int quartoOttetto)
	{
		if(primoOttetto == 224 && secondoOttetto == 0 && terzoOttetto == 0)
			if((quartoOttetto <= 22 && quartoOttetto != 3 && quartoOttetto != 7 && quartoOttetto != 8 && quartoOttetto != 11 && quartoOttetto != 12 && quartoOttetto != 14 && quartoOttetto != 15 && quartoOttetto != 16 && quartoOttetto != 17)
					|| quartoOttetto == 102 || quartoOttetto == 107 || quartoOttetto == 251 || quartoOttetto == 252 || quartoOttetto == 253)
				return true;
		
		return false;
	}
	
	public void ChiudiChatroom(InetAddress indirizzo)
	{
		synchronized (indirizziUDPinUso)
		{
			if(!indirizziUDPinUso.contains(indirizzo))
				throw new NoSuchElementException("La room in questione non è in uso!");
		
		indirizziUDPinUso.remove(indirizzo);
		}
	}
	
	public void SpegnimentoMorbido()
	{
		// Termino gestore registrazioni nuovi account
		try { while(UnicastRemoteObject.unexportObject(gestoreRegistrazioni, false) != true); }
		catch (NoSuchObjectException e) { }
		
		// Termino thread accettazione connessioni e attendo chiusura
		threadAccett.interrupt();
		try { threadAccett.join(); }
		catch (InterruptedException e1) { e1.printStackTrace(); }
		
		// Indico al pool di non accettare più nuovi task
		pool.shutdown();
		
		// Chiudo socket di accettazione connessioni
		try { welcomeSock.close(); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	// Va chiamato esclusivamente DOPO aver invocato SpegnimentoMorbido
	// Restituisce il numero di task che erano in coda nel pool e sono stati eliminati
	public int SpegnimentoImmediato()
	{
		List<Runnable> taskEliminati = pool.shutdownNow();
		
		// Chiudo i socket di comunicazione con i client ancora loggati, per evitare che alcuni thread del pool rimasti bloccati
		// nella read() di un comando dal socket non si accorgano dell'interruzione ricevuta
		for(UtenteLatoServer user : utentiEsistenti)
			if(user.getStato() != UtenteLatoServer.StatiUtente.Offline)
				try { user.ChiudiSocketPrincipale(); }
				catch (IOException e) { }

		return taskEliminati.size();
	}
	
	// Procedura da chiamare dopo aver invocato SpegnimentoMorbido ed aver atteso la terminazione del pool
	public void ConcludiSpegnimento()
	{
		File databaseFolder = new File(config.FILEBASEPATH);
		try { CancellaCartella(databaseFolder); }
		catch (IOException e)
		{
			System.err.println("Errore nello svuotamento della cartella del database documenti: " + config.FILEBASEPATH);
			e.printStackTrace();
		}
	}
	
	
	//======= METODI AUSILIARI =======
	
	// Elimina una cartella con tutti i suoi contenuti
	private void CancellaCartella(File cartella) throws IOException
	{
	    File[] contenuto = cartella.listFiles();
	    if(contenuto != null) //Se non è vuota
	        for(File f: contenuto)
	        {
	            if(f.isDirectory()) CancellaCartella(f);
	            else { Boolean check = f.delete(); if(check == false) throw new IOException("Impossibile eliminare il file " + f.getPath()); }
	        }
	    if(cartella.exists() && cartella.delete() == false) throw new IOException("Impossibile eliminare la cartella " + cartella.getPath());
	}
}
