package turing.tipidato;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/* 
 *		UtenteLatoServer
 * Questa classe rappresenta il descrittore di un utente registrato al servizio TURING,
 * comprensivo di tutte le strutture di book-keeping necessarie sul lato server per tenere traccia della sua attività.
 */

public class UtenteLatoServer {
	
	public enum StatiUtente
	{
		Offline,
		Online,
		Editing
		// Stato in cui l'utente si trova quando sta modificando una sezione di qualche documento
		// (serve per evitare che editi più di una sezione alla volta)
	}

	private final String username;
	// Nome utente dell'account. Univoco e non modificabile (per questo è final).
	
	private final String password;
	// Password dell'account (memorizzata in chiaro). Non modificabile (per questo è final).
	
	private StatiUtente statoUtente;
	// Lo stato in cui si trova l'utente (offline, online, editing)
	
	private DocumentoLatoServer documentoAttualmenteEditato;
	// Riferimento al documento che l'utente sta modificando al momento. Significativo solo se lo stato è "editing".
	
	private int sezioneAttualmenteEditata;
	// Numero della sezione editata nel documento documentoAttualmenteEditato. Significativo solo se lo stato è "editing".
	
	private Set<DocumentoLatoServer> DocumentiCreati;
	// Insieme dei documenti di cui l'utente è creatore
	
	private Set<DocumentoLatoServer> DocumentiDoveEStatoInvitato;
	// Insieme dei documenti dei quali l'utente non è creatore, ma è collaboratore
	// (tutti i documenti creati da altri ma condivisi con lui tramite share, per i quali ha ricevuto l'invito a collaborare)
	
	private Socket sockPrincipale;
	// Socket TCP usato per la connessione principale. Serve a chiuderlo in caso di disconnessione del client,
	// per sbloccare i metodi sospesi nella sua lettura (metodo ChiudiSocketPrincipale).
	
	private Socket sockNotifiche;
	// Socket TCP per inviare le notifiche all'utente (quando qualcuno invoca una share).
	// Allocato al login; vale null quando l'utente non è loggato.
	
	private ObjectOutputStream streamNotifiche;
	
	private Vector<Documento> condivisioniPendenti;
	//Insieme degli inviti pendenti (documenti condivisi con lui mentre era offline, e che devono essergli notificati al prossimo login)
	
	//======= COSTRUTTORE =======
	
	public UtenteLatoServer(String username, String password)
	{
		if(username == null || username.isEmpty()) throw new IllegalArgumentException("Impossibile allocare una variabile utente con username vuoto!");
		if(password == null || password.isEmpty()) throw new IllegalArgumentException("Impossibile allocare una variabile utente con password vuota!");
		
		this.username = username;
		this.password = password;
		this.DocumentiCreati = new HashSet<DocumentoLatoServer>();
		this.DocumentiDoveEStatoInvitato = new HashSet<DocumentoLatoServer>();
		documentoAttualmenteEditato = null;
		sezioneAttualmenteEditata = -1;
		sockPrincipale = null;
		sockNotifiche = null;
		condivisioniPendenti = new Vector<Documento>();
		statoUtente = StatiUtente.Offline;
	}
	
	//======= METODI GETTER =======
	public String getNome()
	{
		return new String (username);
	}
	
	public Boolean PasswordCorretta(String tentativoPassword)
	{
		if(password == null || password.isEmpty()) throw new IllegalArgumentException();
		
		return password.equals(tentativoPassword);
	}

	synchronized public StatiUtente getStato()
	{
		StatiUtente retval = statoUtente;
		return retval;
	}
	
	synchronized public Set<DocumentoLatoServer> getDocumentiCreati()
	{
		return Collections.unmodifiableSet(DocumentiCreati);
	}
	
	synchronized public Set<DocumentoLatoServer> getDocumentiDoveCollabora()
	{
		return Collections.unmodifiableSet(DocumentiDoveEStatoInvitato);
	}
	
	synchronized public DocumentoLatoServer getDocumentoAttualmenteEditato()
	{
		if(statoUtente != StatiUtente.Editing) return null;
		return documentoAttualmenteEditato;
	}
	
	synchronized public int getSezioneAttualmenteEditata()
	{
		if(statoUtente != StatiUtente.Editing) return -1;
		return sezioneAttualmenteEditata;
	}
	
	//======= METODI SETTER =======
	
	// Cambia lo stato dell'utente in "loggato" e invia le notifiche pendenti
	synchronized public void RegistraLogin(Socket sockPrincipale, Socket sockNotifiche) throws IOException
	{
		if(sockNotifiche == null) throw new IllegalArgumentException();
		
		statoUtente = StatiUtente.Online;
		this.sockPrincipale = sockPrincipale;
		this.sockNotifiche = sockNotifiche;
		this.streamNotifiche = new ObjectOutputStream(this.sockNotifiche.getOutputStream());
		
		if(condivisioniPendenti.size() == 0)
			// Se non ci sono notifiche pendenti da inviare, invio notifica vuota di "inizio trasmissioni".
			// (necessaria come meccanismo di sincronizzazione nel client, vedi classe GestoreNotifiche nel client)
			inviaNotifica(new Documento("", ""));
		else
		{
			// Invio le notifiche pendenti
			inviaNotifiche(new Vector<Documento>(condivisioniPendenti));
			condivisioniPendenti.clear();
		}
	}
	
	synchronized public void RegistraLogout()
	{		
		statoUtente = StatiUtente.Offline;
		sockPrincipale = null;
		sockNotifiche = null;
		streamNotifiche = null;
	}
	
	synchronized public void RegistraInizioModifica(DocumentoLatoServer doc, int numSezione)
	{
		if(numSezione < 0 || doc == null) throw new IllegalArgumentException();
		
		statoUtente = StatiUtente.Editing;
		documentoAttualmenteEditato = doc;
		sezioneAttualmenteEditata = numSezione;
	}
	
	synchronized public void RegistraFineModifica()
	{		
		statoUtente = StatiUtente.Online;
		documentoAttualmenteEditato = null;
		sezioneAttualmenteEditata = -1;
	}
	
	
	
	synchronized public void RegistraDocumentoCreato(DocumentoLatoServer documento)
	{
		if(documento == null) throw new IllegalArgumentException();
		
		DocumentiCreati.add(documento);
	}
	
	synchronized public void RegistraDocumentoCondiviso(DocumentoLatoServer documento) throws SocketException
	{
		if(documento == null || documento.getNome() == null || documento.getCreatore() == null) throw new IllegalArgumentException();
		
		DocumentiDoveEStatoInvitato.add(documento);
		
		Documento docPerNotifica = new Documento(documento.getNome(), documento.getCreatore());
		if(statoUtente == StatiUtente.Offline)
			condivisioniPendenti.add(docPerNotifica);
		else
			inviaNotifica(docPerNotifica);
	}
	
	synchronized private void inviaNotifica(Documento documentoCondiviso) throws SocketException
	{
		try {
			Vector<Documento> insieme = new Vector<Documento>();
			insieme.add(documentoCondiviso);
			streamNotifiche.writeObject(insieme);
		} catch(IOException e) { e.printStackTrace(); }
	}
	
	synchronized private void inviaNotifiche(Vector<Documento> documenti)
	{
		try {
			streamNotifiche.writeObject(documenti);
		} catch(IOException e) { e.printStackTrace(); }
	}
	
	public void ChiudiSocketPrincipale() throws IOException
	{
		if(sockPrincipale != null && !sockPrincipale.isClosed())
			sockPrincipale.close();
	}
}
