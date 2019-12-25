/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.server;

import turing.tipidato.*;
import turing.tipidato.UtenteLatoServer.StatiUtente;
import turing.exceptions.*;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/* 
 *		TaskEsecuzione
 * Questa classe definisce il concetto di sessione utente (intesa come
 * il servizio di uno stesso utente nell'intervallo compreso tra login e logout).
 * Costituisce il task che viene sottoposto ed eseguito dal thread pool alla
 * ricezione di una nuova connessione.
 * Contiene pertanto il codice dei thread appartenenti al pool (in particolare,
 * il ciclo in cui si attendono e si interpretano le richieste, e i vari metodi
 * che si interfacciano col server per eseguire i diversi comandi).
 */

public class TaskEsecuzione implements Runnable
{	
	private final ConfigurazioneServer configurazione;
	
	//======= VAR DI ISTANZA =======
	
	private final TuringServer server;
	
	private UtenteLatoServer utente;
	
	private final Socket sockPrincipale;
	// Socket TCP per comunicare normalmente con l'utente (invio di comandi e risposte)
	
	private final ObjectOutputStream streamInvio;
	
	private final ObjectInputStream streamRicezione;
	
	private final Socket sockNotifiche;
	// Socket TCP per inviare le notifiche all'utente (quando qualcuno invoca una share)
	
	//======= COSTRUTTORE =======
	
	public TaskEsecuzione(TuringServer server, Socket sockPrincipale, ObjectInputStream streamRicezione, ObjectOutputStream streamInvio, Socket sockNotifiche, ConfigurazioneServer configurazione)
	{
		this.sockPrincipale = sockPrincipale;
		this.sockNotifiche = sockNotifiche;
		this.server = server;
		this.configurazione = configurazione;
		this.utente = null;
		this.streamInvio = streamInvio;
		this.streamRicezione = streamRicezione;
	}
	
	//======= CODICE THREAD =======

	@Override
	public void run()
	{
		// Implicitamente, la prima richiesta è sempre quella di login
		try { AccediAccount(); }
		catch (NoSuchUserException | AlreadyLoggedException | WrongPasswordException e)
		{
			//Login non riuscito: termino
			InviaRispostaErrore(e);
			ChiudiConnessione();
			return;
		}
		catch (SocketException | EOFException e) { ChiudiConnessione(); return; } // Il client si è disconnesso: termino
		catch (IOException e) { e.printStackTrace(); ChiudiConnessione(); return; } // Errore di comunicazione: termino

		// Ciclo leggi richiesta -> invoca metodo esecutore
		while(!Thread.interrupted())
		{
			try
			{
				String comando = streamRicezione.readUTF();
				if(comando == null || comando.isEmpty()) continue;
				
				switch(comando) {
					case "logout":
					{
						EsciAccount();
						ChiudiConnessione();
						return;
					}
					case "create":
					{
						CreaDocumento();
						continue;
					}
					case "share":
					{
						CondividiDocumento();
						continue;
					}
					case "showsect":
					{
						VisualizzaSezione();
						continue;
					}
					case "showentire":
					{
						VisualizzaInteroDocumento();
						continue;
					}
					case "list":
					{
						ElencaDocumenti();
						continue;
					}
					case "edit":
					{
						IniziaModifica();
						continue;
					}
					case "end-edit":
					{
						TerminaModifica();
						continue;
					}
					default:
					{
						System.err.println("[Server] Errore fatale: ricevuto dal client il seguente comando non riconosciuto: " + comando);
						ChiudiConnessione();
						return;
					}
				}
			}
			catch (SocketException | EOFException e)
			{
				// Il client si è disconnesso segnalando shutDownOutput sul socket, ma senza effettuare il logout				
				if(utente.getStato() == StatiUtente.Editing)
				{
					DocumentoLatoServer doc = utente.getDocumentoAttualmenteEditato();
					InetAddress chatroomDaLiberare = doc.RegistraFineModifica(utente.getSezioneAttualmenteEditata(), -1);
					if(chatroomDaLiberare != null) server.ChiudiChatroom(chatroomDaLiberare);
					
					utente.RegistraFineModifica();
				}
				utente.RegistraLogout();
				ChiudiConnessione();
				return;
			}
			catch (IOException e) { e.printStackTrace(); ChiudiConnessione(); return; } //Errore di comunicazione irreparabile
			catch (EccezioneTuring e) { InviaRispostaErrore(e); } //Richiesta irricevibile: rispondo e si aspetta la prossima
		}
	}
	
	//======= METODI CHE ESEGUONO I COMANDI =======
	
	private void AccediAccount()
			throws NoSuchUserException, WrongPasswordException, AlreadyLoggedException, SocketException, IOException
	{
		// Ricezione parametri
		String username = streamRicezione.readUTF();
		String password = streamRicezione.readUTF();
		
		// Esecuzione		
		if(!server.EsisteUtente(username)) throw new NoSuchUserException("Il nome utente specificato non risulta registrato!");
		utente = server.GetUtente(username);
		
		if(utente.getStato() != StatiUtente.Offline)
			throw new AlreadyLoggedException("Non si può utilizzare la stessa utenza da più client contemporaneamente!\nQualcun altro, tramite un altro client, ha già effettuato l'accesso con l'account '" + username + "'");
		
		if(!utente.PasswordCorretta(password)) throw new WrongPasswordException("La password non è corretta!\nRiprova");
		
		utente.RegistraLogin(sockPrincipale, sockNotifiche);
		
		// Invio risposta
		streamInvio.writeBoolean(true); streamInvio.flush();
	}

	private void EsciAccount() throws NotLoggedException, EditingException, IOException
	{
		// Esecuzione
		if(utente.getStato() == StatiUtente.Offline) throw new NotLoggedException("Non sei loggato all'interno di alcun account!\nPer eseguire il comando devi prima eseguire il login.");
		if(utente.getStato() == StatiUtente.Editing) throw new EditingException("Sei in modalità modifica!\nConcludi la modifica del documento prima di effettuare il logout.");
		
		utente.RegistraLogout();
		
		// Invio risposta
		streamInvio.writeBoolean(true); streamInvio.flush();
	}

	private void CreaDocumento()
			throws NotLoggedException, EditingException, IllegalArgumentException,
			UnavailableNameException, NonPositiveNumberException, IOException
	{
		// Ricezione parametri
		String docname = streamRicezione.readUTF();
		int numSezioni = streamRicezione.readInt();

		// Esecuzione
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException("Devi accedere a un account per poter creare un documento!");
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Editing) throw new EditingException("Non puoi creare un documento mentre ne modifichi un altro!");
		
		if(numSezioni <= 0) throw new NonPositiveNumberException("Il numero di sezioni deve essere strettamente positivo (avevi inserito '" + numSezioni + "')");
		if(server.EsisteDocumento(docname)) throw new UnavailableNameException("Il nome scelto (" + docname + ") non è disponibile (esiste già un altro documento con questo nome).\nProva a scegliere un nome diverso!");
		
		DocumentoLatoServer doc = new DocumentoLatoServer(docname, utente.getNome(), numSezioni);
		server.aggiungiDocumento(doc);
		utente.RegistraDocumentoCreato(doc);
		
		// Inizializzo strutture su file system per il documento
		File cartellaDocumento = new File(PercorsoFileSystem(docname, -1));
		if(cartellaDocumento.mkdir() == false) throw new IOException("Impossibile creare la cartella del nuovo documento: " + cartellaDocumento.getPath());
		for(int i=0; i<numSezioni; i++)
		{
			File sezione = new File(PercorsoFileSystem(docname, i));
			if(sezione.createNewFile() == false) throw new IOException("Impossibile inizializzare il file della sezione " + i + " del documento: " + sezione.getPath());
		}
		
		// Invio risposta
		streamInvio.writeBoolean(true); streamInvio.flush();
	}

	private void CondividiDocumento()
		throws NotLoggedException, EditingException, NoSuchDocumentException, NoSuchUserException, UserRightsException, SocketException, IOException
	{
		// Ricezione parametri
		String docname = streamRicezione.readUTF();
		String username = streamRicezione.readUTF();

		// Esecuzione
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException("Devi accedere a un account per poter condividere un documento!");
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Editing) throw new EditingException("Non puoi eseguire una condivisione durante la modifica di un documento!");
		
		if(!server.EsisteDocumento(docname))
			throw new NoSuchDocumentException("Non esiste nessun documento di nome '" + docname + "'!");
		DocumentoLatoServer doc = server.GetDocumento(docname);
		
		if(!doc.getCreatore().equals(utente.getNome()))
			throw new UserRightsException("Non hai i permessi necessari. Solo il creatore del documento (" + doc.getCreatore() + ") può condividerlo con altri utenti!");
		if(doc.getCreatore().equals(username))
			throw new UserRightsException("L'utente " + username + " è il creatore del documento '" + docname + "':\nNon è possibile (e non serve a nulla!) aggiungerlo tra i collaboratori");
		
		if(!server.EsisteUtente(username))
			throw new NoSuchUserException("Non esiste nessun utente registrato di nome '" + username + "'!");
		
		server.GetUtente(username).RegistraDocumentoCondiviso(doc);
		doc.AggiungiCollaboratore(username);
				
		// Invio risposta
		streamInvio.writeBoolean(true); streamInvio.flush();
	}

	private void ElencaDocumenti() throws NotLoggedException, EditingException, IOException
	{
		// Esecuzione
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException("Non sei loggato all'interno di alcun account!\nPer visualizzare la lista esegui prima l'accesso.");
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Editing) throw new EditingException("Non puoi visualizzare l'elenco durante la modifica di un documento!");
		
		Set<DocumentoLatoServer> creati = utente.getDocumentiCreati();
		Set<DocumentoLatoServer> invitato = utente.getDocumentiDoveCollabora();
		
		Set<DocumentoLatoServer> unione = new HashSet<DocumentoLatoServer>();
		unione.addAll(creati); unione.addAll(invitato);
		
		DocumentoConDettagli[] valoreInvio = new DocumentoConDettagli[unione.size()];
		
		int count = 0;
		for(DocumentoLatoServer doc : unione)
		{
			valoreInvio[count] = new DocumentoConDettagli(doc);
			count++;
		}
		
		// Invio risposta
		streamInvio.writeBoolean(true);
		streamInvio.writeObject(valoreInvio);
		streamInvio.flush();
	}
	
	private void VisualizzaSezione()
			throws NotLoggedException, EditingException, NoSuchDocumentException, NoSuchSectionException, UserRightsException, IOException
	{
		// Ricezione parametri
		String docname = streamRicezione.readUTF();
		int numSezione = streamRicezione.readInt();
		
		// Esecuzione
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException();
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Editing) throw new EditingException("Non puoi visualizzare documenti mentre ne modifichi un altro!");
		
		if(!server.EsisteDocumento(docname))
			throw new NoSuchDocumentException("Non esiste alcun documento di nome '" + docname + "'!");
		DocumentoLatoServer documento = server.GetDocumento(docname);
		
		if(numSezione < 0 || numSezione >= documento.getNumSezioni())
			throw new NoSuchSectionException("Il documento " + docname + " non possiede una sezione numero " + numSezione + "!\n(ricorda che la numerazione delle sezioni parte da zero e non da uno)");

		if(!documento.HaPermessi(utente.getNome()))
			throw new UserRightsException("Non hai i permessi necessari. Solo l'autore e i collaboratori possono visualizzare un documento");
		
		synchronized(documento.lockTuttiFile)
		{
			// Invio risposta
			streamInvio.writeBoolean(true); //Esito
			
			streamInvio.writeBoolean(documento.QualcunoStaModificandoLaSezione(numSezione));
										//Flag che indica se è in corso di modifica oppure no
			
			InviaFile(docname, numSezione); //Contenuto file
		}
	}

	private void VisualizzaInteroDocumento()
			throws NotLoggedException, EditingException, NoSuchDocumentException, UserRightsException, IOException
	{
		// Ricezione parametri
		String docname = streamRicezione.readUTF();
		
		// Esecuzione
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException();
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Editing) throw new EditingException("Non puoi visualizzare documenti mentre ne modifichi un altro!");
		
		if(!server.EsisteDocumento(docname))
			throw new NoSuchDocumentException("Non esiste alcun documento di nome '" + docname + "'!");
		DocumentoLatoServer documento = server.GetDocumento(docname);
		
		if(!documento.HaPermessi(utente.getNome()))
			throw new UserRightsException("Non hai i permessi necessari. Solo l'autore e i collaboratori possono visualizzare un documento");
		
		HashSet<Integer> insiemeSezioniInCorsoDiModifica = new HashSet<Integer>();
		for(int sezione=0; sezione<documento.getNumSezioni(); sezione++)
			if(documento.QualcunoStaModificandoLaSezione(sezione)) insiemeSezioniInCorsoDiModifica.add(new Integer(sezione));
		Integer[] arraySezioniInCorsoDiModifica = insiemeSezioniInCorsoDiModifica.toArray(new Integer[insiemeSezioniInCorsoDiModifica.size()]);
		
		synchronized(documento.lockTuttiFile)
		{
			// Invio risposta
			streamInvio.writeBoolean(true); //Esito
			streamInvio.writeObject(arraySezioniInCorsoDiModifica); //Elenco delle sezioni in corso di modifica
			
			InviaFile(docname, -1); //Contenuto
		}
	}

	private void IniziaModifica()
			throws NotLoggedException, EditingException, NoSuchDocumentException,
			NoSuchSectionException, UserRightsException, OtherUserEditingException, IOException
	{
		// Ricezione parametri
		String docname = streamRicezione.readUTF();
		int numSezione = streamRicezione.readInt();
		
		// Esecuzione		
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException("Per modificare un documento devi effettuare l'accesso ad un account!");
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Editing) throw new EditingException("Non puoi modificare un documento mentre ne stai già modificando un altro!");
		
		if(!server.EsisteDocumento(docname)) throw new NoSuchDocumentException("Non esiste alcun documento di nome '" + docname + "'!");
		DocumentoLatoServer documento = server.GetDocumento(docname);
		
		if(!documento.HaPermessi(utente.getNome()))
			throw new UserRightsException("Non hai i permessi necessari. Solo l'autore e i collaboratori possono modificare un documento");
		if(numSezione < 0 || numSezione >= documento.getNumSezioni())
			throw new NoSuchSectionException("Il documento '" + docname + "' non possiede una sezione numero " + numSezione + "!\n(ricorda che la numerazione delle sezioni parte da zero e non da uno)");
		if(documento.QualcunoStaModificandoLaSezione(numSezione))
			throw new OtherUserEditingException("C'è già l'utente " + documento.ChiStaModificandoLaSezione(numSezione) + " che sta modificando la sezione numero "
					+ numSezione + " del documento '" + docname + "'!\nAttendi che finisca e riprova");
		
		if(!documento.CiSonoSezioniInModifica())
		{		//Creo la chat
			InetAddress chat = server.GeneraChatroom();
			documento.RegistraInizioModifica(utente.getNome(), numSezione, chat);
		}
		else	//La chat esiste già
			documento.RegistraInizioModifica(utente.getNome(), numSezione, null);
		
		utente.RegistraInizioModifica(documento, numSezione);
		
		// Invio risposta
		streamInvio.writeBoolean(true);
		streamInvio.writeObject(documento.GetIndirizzoChat());
		
		InviaFile(docname, numSezione);
	}

	private void TerminaModifica() throws NotLoggedException, NotEditingException, IOException
	{
		// Ricezione parametri
		String docname = streamRicezione.readUTF();
		int numSezione = streamRicezione.readInt();
				
		// Esecuzione
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Offline) throw new NotLoggedException("Per modificare un documento devi effettuare l'accesso ad un account!");
		if(utente.getStato() == UtenteLatoServer.StatiUtente.Online) throw new NotEditingException("Per concludere una modifica devi prima avviarla!");
		
		// Controllo che sezione e documento richiesti combacino con quelli che l'utente stava effettivamente modificando
		String realDocName = utente.getDocumentoAttualmenteEditato().getNome();
		if(!realDocName.equals(docname) || numSezione != utente.getSezioneAttualmenteEditata())
			throw new NotEditingException("Non hai mai chiesto di modificare la sezione " + numSezione + " del documento '" +
					docname + "':\nquella che stavi modificando è la sezione " + utente.getSezioneAttualmenteEditata() +
					" del documento '" + realDocName + "'.\nRiprova specificando i dati corretti!");
		
		DocumentoLatoServer documento = server.GetDocumento(docname);
		
		synchronized(documento.lockTuttiFile)
		{
			// Invio risposta
			streamInvio.writeBoolean(true); streamInvio.flush();
			
			// Ricevo la lunghezza e il contenuto
			long fileSize = streamRicezione.readLong();
			RiceviEAggiornaFile(docname, numSezione, fileSize);
			
			// Registro la conclusione della scrittura
			InetAddress chatroomDaChiudere = documento.RegistraFineModifica(numSezione, fileSize);
			if(chatroomDaChiudere != null) server.ChiudiChatroom(chatroomDaChiudere);
			utente.RegistraFineModifica();
		}
	}
	
	//======= METODI AUSILIARI =======
	
	private <T extends EccezioneTuring & Serializable> void InviaRispostaErrore(T eccezione)
	{
		try
		{
			streamInvio.writeBoolean(false);
			streamInvio.writeObject(eccezione);
			streamInvio.flush();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	private void ChiudiConnessione()
	{
		try
		{
			//Chiudo connessione principale
			streamInvio.close();
			streamRicezione.close();
			if(!sockPrincipale.isClosed())
			{
				if(!sockPrincipale.isOutputShutdown())
					sockPrincipale.shutdownOutput();
				
				if(!sockPrincipale.isInputShutdown())
					sockPrincipale.shutdownInput();
				
				sockPrincipale.close();
			}
			
			//Chiudo connessione notifiche
			if(!sockNotifiche.isClosed())
			{
				if(!sockNotifiche.isOutputShutdown())
					sockNotifiche.shutdownOutput();
				
				sockNotifiche.close();
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	// Se numSezione è positivo o nullo, restituisce il percorso del file che indica la corrispondente sezione del documento;
	// se è negativo, restituisce il percorso della cartella del documento
	private String PercorsoFileSystem(String docname, int numSezione)
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
	
	private void InviaFile(String docname, int sezione) throws IOException
	// Procedura ausiliare che invia al client il testo di un documento.
	// Se sezione è >= 0 viene inviata solo la relativa sezione; se è -1 viene inviato l'intero documento. 
	{
		// Individuo i file da inviare
		String[] elencoPathDaInviare;
		long dimensioneInvio = 0;
		if(sezione >= 0)
		{
			elencoPathDaInviare = new String[1];
			elencoPathDaInviare[0] = PercorsoFileSystem(docname, sezione);
			
			File file = new File(elencoPathDaInviare[0]);
			if(!file.exists() || !file.canRead()) System.err.println("Inaspettatamente il file da inviare ("+ elencoPathDaInviare[0] + ") non esiste o non può essere letto!");
			dimensioneInvio = file.length();
		}
		else
		{
			int totaleSezioni = server.GetDocumento(docname).getNumSezioni();
			
			elencoPathDaInviare = new String[totaleSezioni];
			for(int i=0; i<totaleSezioni; i++)
			{
				elencoPathDaInviare[i] = PercorsoFileSystem(docname, i);
				File file = new File(elencoPathDaInviare[i]);
				if(!file.exists() || !file.canRead()) System.err.println("Inaspettatamente il file da inviare ("+ elencoPathDaInviare[i] + ") non esiste o non posso leggerlo!");
				dimensioneInvio += file.length();
			}
		}
		
		// Spedisco indicazione della lunghezza dei dati spediti
		streamInvio.writeLong(dimensioneInvio); streamInvio.flush();
		
		// Spedisco i file invidivuati
		WritableByteChannel outputChan = Channels.newChannel(streamInvio);
		
		for(String filePath : elencoPathDaInviare)
		{			
			FileChannel inputChan = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
			inputChan.transferTo(0, inputChan.size(), outputChan);
			
			inputChan.close();
		}
		streamInvio.flush();
	}
	
	private void RiceviEAggiornaFile(String docname, int sezione, long fileSize) throws IOException
	{
		String filePath = PercorsoFileSystem(docname, sezione);
		File file = new File(filePath);
		if(!file.exists() || !file.canWrite()) System.err.println("Inaspettatamente il file su cui scrivere ("+ filePath + ") non esiste o non ci posso scrivere!");
		if(file.delete() == false) System.err.println("Impossibile eliminare il file " + filePath);
		if(file.createNewFile() == false) System.err.println("Impossibile creare il file " + filePath);
		
		ReadableByteChannel inputChan = Channels.newChannel(streamRicezione);
		FileChannel outputChan = FileChannel.open(Paths.get(filePath), StandardOpenOption.WRITE);
		outputChan.transferFrom(inputChan, 0, fileSize);
		outputChan.close();
	}

}
