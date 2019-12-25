/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Client
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.client;

import turing.exceptions.*;
import turing.tipidato.*;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.Vector;

/*
 * 		InoltratoreRichieste
 * Questa classe definisce un oggetto "inoltratore" che si occupa di inviare (inoltrare)
 * richieste sintatticamente valide al server e di ricevere la risposta.
 * Contiene i metodi per inizializzare la connessione al server e per inoltrare i vari tipi di comandi.
 * Si occupa inoltre di creare e gestire i thread di supporto, la struttura delle cartelle sul file system,
 * e di controllare la terminazione del client.
 */


public class InoltratoreRichieste
{
	//======= VAR DI ISTANZA =======
	
	private final ConfigurazioneClient configurazione;
	
	private StatiAutoma statoClient;
	private String username;
	
	private Socket sockPrincipale;
	private ObjectOutputStream streamInvio;
	private ObjectInputStream streamRicezione;
	
	private GestoreChat mioGestoreChat;
	private Thread threadChat;
	
	private GestoreNotifiche mioGestoreNotifiche;
	private Thread ThreadNotifiche;
	
	private enum StatiAutoma
	{
		Started,
		Logged,
		Editing
	}
	
	//======= COSTRUTTORE =======
	
	public InoltratoreRichieste(ConfigurazioneClient configurazione)
	{
		this.configurazione = configurazione;
		
		sockPrincipale = null;
		streamInvio = null;
		streamRicezione = null;
		
		mioGestoreChat = null;
		threadChat = null;
		mioGestoreNotifiche = null;
		ThreadNotifiche = null;
		
		statoClient = StatiAutoma.Started;
		this.username = null;
		
		// Inizializzo la cartella dove saranno salvati i documenti scaricati 
		try {
			// Creo la cartella base dei download in caso non esista, e controllo i diritti di scrittura
			File cartellaDownload = new File(configurazione.FILEBASEPATH);
			
			if(!cartellaDownload.exists())
				if(cartellaDownload.mkdir() == false) throw new IOException("Impossibile creare la cartella " + configurazione.FILEBASEPATH);
			if(!cartellaDownload.canWrite())
				throw new IOException("Il programma non dispone dei diritti per scrivere sulla cartella dell'applicativo (" + cartellaDownload.getPath() + ")!");
			
			// Ripulisco la cartella base dalle sottocartelle sulle istanze vecchie (residuo di esecuzioni precedenti terminate in via anomala)
			for(String cartellaIstanza : cartellaDownload.list())
				CancellaCartellaSessioneSeInutilizzata(new File(configurazione.FILEBASEPATH + cartellaIstanza));
			
			// Creo la sottocartella per questa istanza del client e il file segnadata
			int NumeroIstanza = 0;
			File cartellaIstanza;
			
			do {
				NumeroIstanza++;
				cartellaIstanza = new File(configurazione.FILEBASEPATH + "Istanza" + NumeroIstanza + File.separator);
			} while(cartellaIstanza.exists());
			
			configurazione.FILEBASEPATH += "Istanza" + NumeroIstanza + File.separator;
			if(cartellaIstanza.mkdir() == false) throw new IOException("Impossibile creare la cartella " + cartellaIstanza.getPath());
			
			configurazione.FileSegnadataIstanza = new File(configurazione.FILEBASEPATH + ".DataSessione");
			if(configurazione.FileSegnadataIstanza.createNewFile() == false)
				throw new IOException("Impossibile creare il file " + configurazione.FileSegnadataIstanza.getPath());
			RegistraUsoSessione();
		}
		catch (IOException e) {
			System.err.println("Impossibile avviare correttamente il client:\nNon è stato possibile inizializzare la cartella dei documenti scaricati nel file system");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	//======= METODI AUSILIARI =======
	
	// Se numSezione è positivo o nullo, restituisce il percorso del file che indica la corrispondente sezione del documento;
	// se è negativo, restituisce il percorso della cartella del documento
	public String PercorsoFileSystem(String docname, int numSezione)
	{
		String nomeDepuratoCartellaDocumento = null;

		try { nomeDepuratoCartellaDocumento = URLEncoder.encode(docname, "UTF-8"); }
		catch (UnsupportedEncodingException e) { e.printStackTrace(); } //Non si verificherà (UTF-8 è una codifica nota)
			
		if(docname.equals(".") || docname.equals("..")) nomeDepuratoCartellaDocumento = docname.replaceAll(".", "%PUNTO");
		
		if(numSezione < 0)
			return configurazione.FILEBASEPATH + nomeDepuratoCartellaDocumento + File.separator;
		else
			return configurazione.FILEBASEPATH + nomeDepuratoCartellaDocumento + File.separator + "sez" + numSezione + ".txt";
	}
	
	// Rinnova la data di ultima modifica nel "file segnadata", per segnalare che la cartella dei file scaricati di questa sessione è ancora in uso
	public void RegistraUsoSessione()
	{
		if(configurazione.FileSegnadataIstanza.setLastModified(System.currentTimeMillis()) == false)
			System.err.println("Impossibile aggiornare la data di ultima modifica del file segnadata " + configurazione.FileSegnadataIstanza.getPath());
	}
	
	// Controlla se la cartella passata per parametro, relativa ai file scaricati relativa di certa sessione,
	// è stata usata di recente o no, e in questo caso la cancella 
	private void CancellaCartellaSessioneSeInutilizzata(File cartella) throws IOException
	{
		if(cartella == null || !cartella.exists() || !cartella.isDirectory()) return;
		
		final File fileSegnadata = new File(cartella.getPath() + File.separator + ".DataSessione");
		final long UNORA = 3600000;
		
		if(!fileSegnadata.exists())
			CancellaCartella(cartella);
		else {
			long etaOre = (System.currentTimeMillis() - fileSegnadata.lastModified()) / UNORA;
			if(etaOre >= 12) CancellaCartella(cartella);
		}
	}
	
	// Elimina una cartella con tutti i suoi contenuti
	public void CancellaCartella(File cartella) throws IOException
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
	
	// Se invocato con parametro "sezione" negativo, scarica l'intero file
	private void ScaricaFile(String docname, int sezione) throws IOException
	{
		// Creo la sottocartella del documento
		File cartellaDocumento = new File(PercorsoFileSystem(docname, -1));
		if(!cartellaDocumento.exists())
			if(cartellaDocumento.mkdir() == false)
				throw new IOException("Impossibile creare la cartella del nuovo documento: " + cartellaDocumento.getPath());
		
		// Creo il file della sezione (eliminando eventuali omonimi preesistenti)
		String filePath;
		if(sezione >= 0) filePath = PercorsoFileSystem(docname, sezione);
		else filePath = PercorsoFileSystem(docname, -1) + "DocumentoCompleto.txt";
		File file = new File(filePath);

		if(file.exists())
			if(file.delete() == false)
				System.err.println("Impossibile eliminare il file " + filePath);
		if(file.createNewFile() == false) System.err.println("Impossibile creare il file " + filePath);
		if(!file.canWrite()) System.err.println("Inaspettatamente non posso scrivere il documento da scaricare sul file "+ filePath);
		
		// Ricevo e scrivo il file
		long fileSize = streamRicezione.readLong();
		
		ReadableByteChannel inputChan = Channels.newChannel(streamRicezione);
		FileChannel outputChan = FileChannel.open(Paths.get(filePath), StandardOpenOption.WRITE);
		outputChan.transferFrom(inputChan, 0, fileSize);
		outputChan.close();
	}
	
	private void InviaFile(String docname, int sezione) throws IOException
	// Procedura ausiliare che invia al server il testo della sezione di un documento 
	{
		if(sezione < 0) return;
		
		String path = PercorsoFileSystem(docname, sezione);
		File file = new File(path);
		if(!file.exists() || !file.canRead()) System.err.println("Inaspettatamente il file del documento da inviare ("+ path + ") non esiste o non può essere letto!");
		
		long dimensioneInvio = file.length();
		
		// Spedisco indicazione della lunghezza dei dati spediti
		streamInvio.writeLong(dimensioneInvio); streamInvio.flush();
		
		// Spedisco i file invidivuati
		WritableByteChannel outputChan = Channels.newChannel(streamInvio);
		
		FileChannel inputChan = FileChannel.open(Paths.get(path), StandardOpenOption.READ);
		inputChan.transferTo(0, inputChan.size(), outputChan);
			
		inputChan.close();
		streamInvio.flush();
	}
	
	public Vector<Documento> getNotifichePendenti()
	{
		if(statoClient == StatiAutoma.Started) return null;
		
		return mioGestoreNotifiche.getNotifichePendenti();
	}
	
	private <T extends EccezioneTuring & Serializable> void ControllaEsito(String messaggioErrore) throws T, IOException, ClassNotFoundException
	{
		Boolean esito = streamRicezione.readBoolean();
		if(esito == false)
		{
			@SuppressWarnings("unchecked")
			T eccezioneRicevuta = (T) streamRicezione.readObject();
			
			System.err.println(messaggioErrore + ":");
			throw eccezioneRicevuta;
		}
	}
	
	// Se siamo in modalità modifica, termina il thread gestore delle notifiche e aspetta la sua terminazione
	// (in modo che venga inviato il messaggio di abbandono della chat)
	public void TerminaThreadChat()
	{
		if(mioGestoreChat == null) return;
		
		threadChat.interrupt();
		System.out.println("Uscita dalla chatroom in corso...");
		try { threadChat.join(); }
		catch (InterruptedException e) { }
		
		mioGestoreChat = null;
	}
	
	//======= METODI CHE ESEGUONO I COMANDI UTENTE =======
	
	public void RegistraUtente(String username, String password)
			throws IllegalArgumentException, UnavailableNameException, AlreadyLoggedException, RemoteException, NotBoundException
	{
		if(statoClient != StatiAutoma.Started) throw new AlreadyLoggedException("Impossibile eseguire la registrazione:\nPer registrare un nuovo account, devi prima fare il logout da quello attuale!");
		
		Registry reg = LocateRegistry.getRegistry(); //Usa il registry situato sul localhost sulla porta di default dei registry (1099)
		CreatoreAccount gestoreRegistrazioni = (CreatoreAccount) reg.lookup(configurazione.nomePubblicoServCreatoreAccount);
		
		gestoreRegistrazioni.RegistraUtente(username, password);
	}
	
	public void AccediAccount(String username, String password) throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient != StatiAutoma.Started)
			throw new AlreadyLoggedException("Impossibile eseguire l'accesso:\nSei già loggato in un account!\nSe vuoi cambiare utenza, fai prima il logout da quella attuale!");
		
		Socket sockNotifiche = null;
		try {
			// Mi connetto al server
			sockPrincipale = new Socket(InetAddress.getByName("localhost"), configurazione.PORTAPRINCIPALE);
			streamInvio = new ObjectOutputStream(sockPrincipale.getOutputStream());
			streamInvio.flush();
			streamRicezione = new ObjectInputStream(sockPrincipale.getInputStream());
			
			// Rimango in attesa di ricevere la connessione dal server, per la ricezione delle notifiche
			ServerSocket listenSock = new ServerSocket(configurazione.PORTANOTIFICHE);
			streamInvio.write(0); //Invio al server segnale di sincronizzazione che indica che sono in ascolto
			streamInvio.flush();
			sockNotifiche = listenSock.accept();
			sockNotifiche.shutdownOutput(); //Rendo il socket unidirezionale (solo server -> client)
			listenSock.close();
		}
		catch (UnknownHostException e) { e.printStackTrace(); } //Non si verificherà (localhost è un indirizzo sempre risolvibile)
		
		streamInvio.writeUTF(username);
		streamInvio.writeUTF(password);
		streamInvio.flush();
		ControllaEsito("Impossibile eseguire l'accesso");
		
		this.statoClient = StatiAutoma.Logged;
		this.username = username;
		
		// Avvio thread gestore notifiche
		mioGestoreNotifiche = new GestoreNotifiche(sockNotifiche);
		ThreadNotifiche = new Thread(mioGestoreNotifiche);
		ThreadNotifiche.setName("Client-Notifiche");
		ThreadNotifiche.setDaemon(true);
		ThreadNotifiche.start();
	}
	
	public String EsciAccount() throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile disconnettersi dall'utenza:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("logout"); streamInvio.flush();
		ControllaEsito("Impossibile disconnettersi dall'utenza");

		statoClient = StatiAutoma.Started;
		String retval = this.username;
		this.username = null;
		
		// Termino i demoni
		ThreadNotifiche.interrupt();
		mioGestoreNotifiche = null;
		
		TerminaThreadChat();
		
		return retval;
	}
	
	public void CreaDocumento(String docname, int numSezioni) throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile creare il documento:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("create");
		
		streamInvio.writeUTF(docname);
		streamInvio.writeInt(numSezioni);
		streamInvio.flush();
		
		ControllaEsito("Impossibile creare il documento");
	}
	
	public void CondividiDocumento(String docname, String username) throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile condividere il documento:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("share");
		
		streamInvio.writeUTF(docname);
		streamInvio.writeUTF(username);
		streamInvio.flush();
		
		ControllaEsito("Impossibile condividere il documento");
	}
	
	public Boolean VisualizzaSezione(String docname, int numSezione) throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile visualizzare il documento:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("showsect");
		
		streamInvio.writeUTF(docname);
		streamInvio.writeInt(numSezione);
		streamInvio.flush();
		
		ControllaEsito("Impossibile visualizzare il documento");
		
		Boolean InCorsoDiModifica = streamRicezione.readBoolean();
		
		ScaricaFile(docname, numSezione);
		
		return InCorsoDiModifica;
	}
	
	public Integer[] VisualizzaInteroDocumento(String docname) throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile visualizzare il documento:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("showentire");
		
		streamInvio.writeUTF(docname);
		streamInvio.flush();
		
		ControllaEsito("Impossibile visualizzare il documento");
		
		Integer[] elencoSezioniInCorsoDiModifica = (Integer[]) streamRicezione.readObject();
		
		ScaricaFile(docname, -1);
		
		return elencoSezioniInCorsoDiModifica;
	}
	
	public DocumentoConDettagli[] ElencaDocumenti() throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile visualizzare la lista dei documenti:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("list");
		streamInvio.flush();
		
		ControllaEsito("Impossibile visualizzare la lista dei documenti");
		
		DocumentoConDettagli[] lista = (DocumentoConDettagli[]) streamRicezione.readObject();
	
		return lista;
	}
	
	public void IniziaModifica(String docname, int numSezione) throws EccezioneTuring, BindException, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile avviare la modifica del documento:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("edit");
		
		streamInvio.writeUTF(docname);
		streamInvio.writeInt(numSezione);
		streamInvio.flush();
		
		ControllaEsito("Impossibile avviare la modifica del documento");
		statoClient = StatiAutoma.Editing;
		
		// Avvio thread ricezione messaggi dalla chat
		InetAddress indirizzoUDPChat = (InetAddress) streamRicezione.readObject();
		
		mioGestoreChat = new GestoreChat(indirizzoUDPChat, configurazione, username, numSezione);
		threadChat = new Thread(mioGestoreChat);
		threadChat.setName("Client-Chat");
		threadChat.setDaemon(true);
		threadChat.start();
		
		// Ricevo il testo della sezione
		ScaricaFile(docname, numSezione);
	}
	
	public void TerminaModifica(String docname, int numSezione) throws EccezioneTuring, IOException, ClassNotFoundException
	{
		if(statoClient == StatiAutoma.Started)
			throw new NotLoggedException("Impossibile concludere la modifica del documento:\nNon sei loggato in alcun account!\nEsegui prima l'accesso ad un'utenza.");
		
		streamInvio.writeUTF("end-edit");

		streamInvio.writeUTF(docname);
		streamInvio.writeInt(numSezione);
		streamInvio.flush();
		
		ControllaEsito("Impossibile concludere la modifica del documento");
		statoClient = StatiAutoma.Logged;
		
		// Termino thread ricezione messaggi dalla chat
		threadChat.interrupt();
		mioGestoreChat = null;
		
		// Invio il testo della sezione
		InviaFile(docname, numSezione);
	}
	
	public void InviaMessaggio(String testo)
			throws NotLoggedException, NotEditingException, IllegalArgumentException, IOException
	{
		if(statoClient == StatiAutoma.Started) throw new NotLoggedException("Non puoi usare la chat se non hai effettuato l'accesso!");
		if(statoClient == StatiAutoma.Logged) throw new NotEditingException("Non puoi usare la chat se non stai modificando un documento!");
		
		LocalDateTime now = LocalDateTime.now();
		@SuppressWarnings("deprecation")
		MessaggioChat msg = new MessaggioChat(new Time(now.getHour(), now.getMinute(), now.getSecond()), username, testo);
		
		mioGestoreChat.InviaMessaggio(msg);
		
	}
	
	public Queue<MessaggioChat> RiceviMessaggi() throws NotLoggedException, NotEditingException
	{
		if(statoClient == StatiAutoma.Started) throw new NotLoggedException("Non puoi usare la chat se non hai effettuato l'accesso!");
		if(statoClient == StatiAutoma.Logged) throw new NotEditingException("Non puoi usare la chat se non stai modificando un documento!");
		
		return mioGestoreChat.GetMessaggi();
	}
}
