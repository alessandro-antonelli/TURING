/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

/*		TURING SERVER
 * Questo programma costituisce il server di uno strumento per l'editing collaborativo di documenti.
 */

package turing;

/* 
 *		MainClass
 * Questa classe contiene il main che viene eseguito all'avvio dell'applicazione server.
 * Si occupa di istanziare l'oggetto server e l'oggetto che genera l'interfaccia
 * a linea di comando e il ciclo "leggi da tastiera/interpreta comando".
 */

public class MainClass
{
	/*
	 *		SINTASSI / PARAMETRI ACCETTATI DAL MAIN:
	 *
	 *	MainClass (senza argomenti, usa le porte di default)
	 *			OPPURE
	 *  MainClass PortaPrincipale PortaNotifiche
	 *
	 *		PortaPrincipale		(int > 1024)		Porta da usare per la connessione al welcome socket del server e la ricezione di richieste.
	 *		PortaNotifiche		(int > 1024)		Porta da usare per inviare ai client le notifiche.
	*/
	public static void main(String[] args)
	{
		ConfigurazioneServer configurazione;
		final String usage = "Errore: il server è stato avviato con argomenti non validi!\n   Uso:\n" +
				"Per usare le porte di default: nessun argomento                          ->   MainClass\n" +
				"Per usare porte personalizzate: due argomenti interi  maggiori di 1024   ->   MainClass PortaPrincipale PortaNotifiche";
		
		//======= LEGGO I PARAMETRI =======
		if(args.length == 0)
			configurazione = new ConfigurazioneServer(-1, -1);
		else if(args.length == 2) try
		{
			int PORTAPRINCIPALE = Integer.parseInt(args[0]);
			int PORTANOTIFICHE = Integer.parseInt(args[1]);
			if(PORTAPRINCIPALE <= 1024 || PORTANOTIFICHE <= 1024) throw new IllegalArgumentException();
			
			configurazione = new ConfigurazioneServer(PORTAPRINCIPALE, PORTANOTIFICHE);
		}
			catch(NumberFormatException e) { System.err.println(usage); return; }
			catch (IllegalArgumentException e) { System.err.println(usage); return; }
		else { System.err.println(usage); return; }
		
		//======= AVVIO I THREAD =======
		TuringServer server = null;
		try { server = new TuringServer(configurazione); }
		catch (Exception e) //In caso di errore nell'inizializzazione del server termino l'esecuzione
		{
			System.err.printf("Errore fatale: impossibile avviare correttamente il server!\n\n");
			System.err.println(e.getMessage());
			System.err.println("Il server è stato terminato.");
			System.err.println("\nSegue lo stack delle invocazioni:");
			if(e.getCause() != null)	e.getCause().printStackTrace();
							else		e.printStackTrace();
			System.exit(1);
		}
		
		CLI InterfacciaUtente = new CLI(server);
		
		InterfacciaUtente.CicloFetchDecode();
		
		//======= TERMINO =======		
		return;
	}

}
