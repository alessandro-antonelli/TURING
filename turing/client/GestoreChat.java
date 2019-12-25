/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Client
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.client;

import turing.tipidato.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;

/*
 * 		GestoreChat
 * Questa classe definisce la struttura utilizzata per gestire la partecipazione del client alla chat,
 * con il codice per la ricezione di messaggi eseguito dal thread dedicato e
 * la coda dei messaggi pendenti acceduta dal thread principale.
 */

public class GestoreChat implements Runnable {
	
	//======= VAR DI ISTANZA =======
	
	private final ConfigurazioneClient configurazione;
	
	private final Queue<MessaggioChat> messaggiPendenti;
	// Coda nella quale vengono memorizzati i messaggi ricevuti e non ancora visualizzati dall'utente
	// Viene scritta dal thread gestore della chat (metodo run);
	// viene letta e svuotata dal thread gestore dell'interfaccia grafica, tramite il metodo GetMessaggi()
	// La sincronizzazione è realizzata tramite l'uso di blocchi synchronized(messaggiPendenti)
	
	private final InetAddress indirizzoUDPChat;
	// Indirizzo della chatroom (gruppo multicast) al quale dobbiamo prendere parte
	
	private final MulticastSocket socket;
	// Socket per la ricezione e l'invio di messaggi sulla chat
	
	private final String username;
	// Utenza per la quale l'oggetto gestisce la chat
	
	private final int sezioneModificata;
	// Numero della sezione in corso di modifica da parte dell'utente
	
	private final Object lockTerminazione;
	// Oggetto di pura sincronizzazione, usato per impedire che si abbandoni il gruppo
	// prima che il messaggio con la notifica di abbandono sia stata spedita
	
	//======= COSTRUTTORE =======
	
	public GestoreChat(InetAddress indirizzoUDPChat, ConfigurazioneClient configurazione, String username, int sezioneModificata)
			throws BindException, SocketException, IOException
	{
		messaggiPendenti = new LinkedList<MessaggioChat>();
		this.indirizzoUDPChat = indirizzoUDPChat;
		this.configurazione = configurazione;
		this.username = username;
		this.sezioneModificata = sezioneModificata;
		lockTerminazione = new Object();
		
		socket = new MulticastSocket(configurazione.PORTAMESSAGGI);
		
		socket.joinGroup(indirizzoUDPChat);
		
		// Imposto il TTL dei datagrammi in uscita ad uno, in modo tale che non escano dalla rete locale e non creino conflitti con altre applicaizoni
		socket.setTimeToLive(1);
		
		// Imposto timeout per la receive(pacchetto), in modo tale che il thread torni spesso alla guardia del while e rimanga reattivo alle interruzioni
		socket.setSoTimeout(10000);
	}
	
	//======= CODICE THREAD =======

	/*
	 * Metodo eseguito dal thread dedicato all'ascolto della chat.
	 */
	@Override
	public void run()
	{
		// Il client ha un thread che rimane per tutta la durata a controllare sul socket multicast UDP;
		// ogni volta che legge un messaggio lo appende inuna coda condivisa (e quindi synchronized)
		// che viene svuotata dal thread main/interfaccia ogni volta che egli stampa i messaggi (all'invocazione di "receive")
		
		// Invio notifica di aggiunta
		try
		{
			LocalDateTime now = LocalDateTime.now();
			@SuppressWarnings("deprecation")
			MessaggioChat notificaJoin = new MessaggioChat(new Time(now.getHour(), now.getMinute(), now.getSecond()), username, 
					"<si è aggiunto alla chat>   (sta modificando la sezione " + sezioneModificata + ")");
			InviaMessaggio(notificaJoin);
		}
		catch (IOException e) { e.printStackTrace(); }
		
		// Ciclo principale di ascolto
		while(!Thread.interrupted())
		{
			try {
				byte[] buffer = new byte[configurazione.MAXMSGLENGTH];
				DatagramPacket pacchetto = new DatagramPacket(buffer, configurazione.MAXMSGLENGTH);
				socket.receive(pacchetto);
				
				ByteArrayInputStream bais = new ByteArrayInputStream(buffer, 0, pacchetto.getLength());
				
				ObjectInputStream in = new ObjectInputStream(bais);
				MessaggioChat msg = (MessaggioChat) in.readObject();
				in.close();
				synchronized(messaggiPendenti)
					{ messaggiPendenti.add(msg); }
			}
			catch (SocketTimeoutException e) { continue; } //Tutto regolare: è scattato il timeout della receive
			catch (ClassNotFoundException e) { System.err.println("Errore fatale: classe del servizio RMI mancante od obsoleta!\nIl client viene chiuso"); e.printStackTrace(); System.exit(1); }
			catch (IOException e) { System.err.println("Errore fatale: errore di comunicazione nella connessione RMI!\nIl client viene chiuso"); e.printStackTrace(); System.exit(1); }
		}
		
		try {
			// Invio notifica di abbandono
			LocalDateTime now = LocalDateTime.now();
			@SuppressWarnings("deprecation")
			MessaggioChat notificaAbbandono = new MessaggioChat(new Time(now.getHour(), now.getMinute(), now.getSecond()), username, 
						"<è uscito dalla chat>   (ha terminato la modifica della sezione " + sezioneModificata + ")");
			InviaMessaggio(notificaAbbandono);
			
			// Lascio il gruppo
			 synchronized(lockTerminazione)
			 {	// Evita che si abbandoni il gruppo prima che il messaggio con la notifica di abbandono sia stata spedita
				socket.leaveGroup(indirizzoUDPChat);
				socket.close();
			 }
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	//======= METODI SETTER =======
	
	public Queue<MessaggioChat> GetMessaggi()
	{
		synchronized(messaggiPendenti)
		{
			if(messaggiPendenti.isEmpty()) return null;
			else
			{
				LinkedList<MessaggioChat> retval = new LinkedList<MessaggioChat>();
				do
				{
					retval.add(messaggiPendenti.remove());
				} while(!messaggiPendenti.isEmpty());
				
				return retval;
			}
		}
	}
	
	public void InviaMessaggio (MessaggioChat msg) throws IOException
	{
		synchronized(lockTerminazione) 
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(configurazione.MAXMSGLENGTH);
			
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(msg);
			out.close();
			
			DatagramPacket pacchetto = new DatagramPacket(baos.toByteArray(), 0, baos.size(), indirizzoUDPChat, configurazione.PORTAMESSAGGI);
			socket.send(pacchetto);
		}
	}

}
