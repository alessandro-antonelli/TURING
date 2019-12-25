/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Client
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

/*		TURING CLIENT
 * Questo programma costituisce il client di uno strumento per l'editing collaborativo di documenti.
*/

package turing.client;

/* 
 *		MainClass
 * Questa classe contiene il main che viene eseguito all'avvio dell'applicazione client.
 * Si occupa di istanziare l'oggetto che genera l'interfaccia a linea di comando e il ciclo "leggi da tastiera/interpreta comando",
 * e quello che si occupa dell'effettiva esecuzione.
 */

public class MainClass {

	/*
	 *		SINTASSI / PARAMETRI ACCETTATI DAL MAIN:
	 *
	 *	MainClass (senza argomenti, usa le porte di default)
	 *			OPPURE
	 *  MainClass PortaPrincipale PortaNotifiche PortaMessaggi
	 *
	 *		PortaPrincipale		(int > 1024)		Porta da usare per la connessione al welcome socket del server e l'invio di richieste.
	 *		PortaNotifiche		(int > 1024)		Porta da usare per ricevere le notifiche.
	 *		PortaMessaggi		(int > 1024)		Porta da usare per l'invio e la ricezione di messaggi sulla chat.
	*/
	public static void main(String[] args)
	{
		ConfigurazioneClient configurazione;
		final String usage = "Errore: il client è stato avviato con argomenti non validi!\n   Uso:\n" +
				"Per usare le porte di default: nessun argomento                          ->   MainClass\n" +
				"Per usare porte personalizzate: tre argomenti interi  maggiori di 1024   ->   MainClass PortaPrincipale PortaNotifiche PortaMessaggi";
		
		//======= LEGGO I PARAMETRI =======
		if(args.length == 0)
			configurazione = new ConfigurazioneClient(-1, -1, -1);
		else if(args.length == 3) try
		{
			int PORTAPRINCIPALE = Integer.parseInt(args[0]);
			int PORTANOTIFICHE = Integer.parseInt(args[1]);
			int PORTAMESSAGGI = Integer.parseInt(args[2]);
			if(PORTAPRINCIPALE <= 1024 || PORTANOTIFICHE <= 1024 || PORTAMESSAGGI <= 1024) throw new IllegalArgumentException();
			
			configurazione = new ConfigurazioneClient(PORTAPRINCIPALE, PORTANOTIFICHE, PORTAMESSAGGI);
		}
			catch(NumberFormatException e) { System.err.println(usage); return; }
			catch (IllegalArgumentException e) { System.err.println(usage); return; }
		else { System.err.println(usage); return; }
		
		//======= AVVIO THREAD =======
		InoltratoreRichieste inoltratore = new InoltratoreRichieste(configurazione);
		
		CLI InterfacciaUtente = new CLI(inoltratore, configurazione);
		
		//======= ATTENDO E INTERPRETO I COMANDI DELL'UTENTE =======
		InterfacciaUtente.CicloFetchDecode();
	
		//======= TERMINO =======		
		System.out.println("Il client TURING è stato chiuso. Arrivederci alla prossima sessione!");
		return;
	}

}
