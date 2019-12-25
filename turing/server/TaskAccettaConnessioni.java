/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/* 
 *		TaskAccettaConnessioni
 * Questa classe definisce il task eseguito dal thread di accettazione
 * delle nuove connessioni, con il codice per rimanere in attesa, eseguire
 * l'handshake, e sottomettere il task della sessione al thread pool.
 */

public class TaskAccettaConnessioni implements Runnable
{
	//======= VAR DI ISTANZA =======
	private final TuringServer server;
	
	private final ConfigurazioneServer config;
	
	private final ServerSocket welcomeSock;
	// Listening socket dove arrivano le richieste di connessione
	
	//======= COSTRUTTORE =======
	
	public TaskAccettaConnessioni(TuringServer server, ConfigurazioneServer configurazione, ServerSocket welcomeSock)
	{
		this.server = server;
		this.config = configurazione;
		this.welcomeSock = welcomeSock;
	}
	
	//======= CODICE THREAD =======

	@Override
	public void run()
	{
		Thread.currentThread().setName("Server-Accettazione connessioni");
		//Imposto timeout per la accept(), in modo tale che il thread sia reattivo alle interruzioni e torni spesso alla guardia del while
		try { welcomeSock.setSoTimeout(2000); }
		catch (SocketException e) { e.printStackTrace(); }
		
		while(!Thread.interrupted())
		{
			try {
				// Rimango in attesa di ricevere la connessione principale
				Socket mainSock = welcomeSock.accept();
				if(mainSock == null) continue;
				
				ObjectInputStream streamRicezione = new ObjectInputStream(mainSock.getInputStream());
				ObjectOutputStream streamInvio = new ObjectOutputStream(mainSock.getOutputStream());
				
				// Mi connetto al client con nuova connessione, per la ricezione delle notifiche
				streamRicezione.read(); //Aspetto il segnale di sincronizzazione che indica l'inizio dell'ascolto da parte del clent
				Socket sockNotifiche = new Socket(InetAddress.getByName("localhost"), config.PORTANOTIFICHE);
				sockNotifiche.shutdownInput(); //Rendo il socket unidirezionale (solo server -> client)
				
				TaskEsecuzione task = new TaskEsecuzione(server, mainSock, streamRicezione, streamInvio, sockNotifiche, config);
				server.pool.execute((Runnable) task);
			}
			catch (SocketTimeoutException e) { continue; /* Scaduto timeout: tutto regolare */ }
			catch (UnknownHostException e) { e.printStackTrace(); }
			catch (ConnectException e) { System.err.println("Errore: comportamento inaspettato del client, ha rifiutato la connessione necessaria per inviargli le notifiche!"); e.printStackTrace(); }
			catch (IOException e) { System.err.println("Errore di comunicazione: non è stato possibile accettare la richiesta di login dal client!"); e.printStackTrace(); }
		}
	}

}
