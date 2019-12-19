/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/* 
 *		CLI
 * Questa classe contiene il codice che realizza la Command Line Interface,
 * interpretando l'input da tastiera.
 */

public class CLI
{
	//======= COSTANTI =======
	
	private final String splashScreen =		"      ############   ---------------------------------\n" +
											"      ## TURING ##   disTribUted collaboRative edItiNG\n" +
											"      ############   ---------------------------------\n" +
											"         ::: Interfaccia di gestione del server :::\n\n" +
											"Il server si è avviato correttamente! Per l'elenco dei comandi digita: help\n";

	private final String usageString =	"I comandi disponibili sono i seguenti:\n" +
										"stats     Stampa statistiche sul funzionamento di TURING\n" +
										"quit      Spegne il server in via ordinaria o 'dolce' (attendendo che le richieste in elaborazione e in coda siano completate)\n" +
										"quitnow   Spegne il server in via rapida o 'brusca' (le richieste in elaborazione vengono terminate e quelle in coda scartate)\n";
	
	//======= VAR DI ISTANZA =======
	
	private final TuringServer server;
	
	//======= COSTRUTTORE =======

	public CLI(TuringServer server)
	{		
		this.server = server;
		Thread.currentThread().setName("Server-Interfaccia utente");
		System.out.println(splashScreen);
	}
	
	//======= METODO PRINCIPALE =======
	
	public void CicloFetchDecode()
	{
		try {
			BufferedReader terminale = new BufferedReader(new InputStreamReader(System.in));
			Boolean terminareCiclo = false;
			while(!terminareCiclo)
			{
				final String rigaInput = terminale.readLine();
				if(rigaInput == null || rigaInput.isEmpty())
					{ System.err.println("Comando non valido\nPer l'elenco dei comandi digita: help\n"); continue;  }
				
				switch (rigaInput)
		        {
			        case "stats":
	        		{
	        			StampaStats();
	        			continue;
	        		}
		        	case "quit":
	        		{
	        			SpegnimentoMorbido(terminale);
	        			terminareCiclo = true;
	        			break;
	        		}
		        	case "quitnow":
	        		{
	        			SpegnimentoImmediato();
	        			terminareCiclo = true;
	        			break;
	        		}
			        case "help":
	        		{
	        			System.out.println(usageString);
	        			continue;
	        		}
		        	default:
	        		{
	        			System.err.println("Comando \"" + rigaInput + "\" inesistente\nPer l'elenco dei comandi digita: help\n");
	        			continue;
	        		}
		        }					
			}

			terminale.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	//======= METODI AUSILIARI =======
	
	private void StampaStats()
	{
		System.out.println("      [Statistiche servizio fornito]");
		int[] statoUtenti = server.getStatoUtenti();
		System.out.printf("Utenti registrati.................. %d\n", statoUtenti[0] + statoUtenti[1] + statoUtenti[2]);
		System.out.printf("Utenti online...................... %d   (di cui %d in modalità modifica)\n", statoUtenti[1] + statoUtenti[2], statoUtenti[2]);
		System.out.printf("Chatroom attive.................... %d\n", server.getNumeroChatroom());
		System.out.printf("Numero documenti memorizzati....... %d\n", server.getNumDocumentiEsistenti());
		
		long dimByte = server.getDimensioneTotale();
		String dimFormattata;
		if(dimByte >= 1099511627776L) dimFormattata = String.format("%.2f TB", dimByte/(1024*1024*1024*1024F));
		else if(dimByte >= 1073741824L) dimFormattata = String.format("%.2f GB", dimByte/(1024*1024*1024F));
		else if(dimByte >= 1048576L) dimFormattata = String.format("%.2f MB", dimByte/(1024*1024F));
		else if(dimByte >= 1024L) dimFormattata = String.format("%.2f KB", dimByte/1024F);
		else dimFormattata = dimByte + " byte";
		System.out.println("Dimensione documenti memorizzati... " + dimFormattata);
		
		System.out.println("\n      [Statistiche thread pool esecutore]");
		System.out.printf("Dimensione attuale del pool........................ %d thread\n", server.pool.getPoolSize());
		System.out.printf("   di cui attivi (a servizio di una connessione)... %d\n", server.pool.getActiveCount());
		System.out.printf("Dimensione massima mai raggiunta................... %d thread\n", server.pool.getLargestPoolSize());
		System.out.printf("Sessioni utente complete (login->logout) servite... %d\n", server.pool.getCompletedTaskCount());
		
		System.out.println("\n      [Configurazione server]");
		System.out.printf("Cartella database documenti... %s\n", server.config.FILEBASEPATH);
		System.out.printf("Porta principale.............. %d\n", server.config.PORTAPRINCIPALE);
		System.out.printf("Porta per le notifiche........ %d\n", server.config.PORTANOTIFICHE);
		System.out.println(); //A capo finale per distanziare da comandi successivi
	}
	
	private void SpegnimentoMorbido(BufferedReader terminale)
	{
		System.out.println("Cominciata la fase di spegnimento ordinario del server. Non vengono più accettate nuove richieste, si finirà di servire quelle già in esecuzione.");
		server.SpegnimentoMorbido();
		
		// Se lo spegnimento dura molto, faccio comparire un messaggio ricorrente che indica quanto manca
		// e propone di passare alla modalità rapida
		Boolean passatoInModalitaRapida = false;
		while(!server.pool.isTerminated() && !passatoInModalitaRapida)
		{
			try {
				server.pool.awaitTermination(5000, TimeUnit.MILLISECONDS);
				if(!server.pool.isTerminated()) System.out.printf("Ci sono ancora %d richieste in esecuzione e %d in attesa di essere servite. Se non vuoi aspettare, digita \"quitnow\" per una terminazione rapida\n", server.pool.getPoolSize(), server.pool.getQueue().size());
				if(terminale.ready() && terminale.readLine().equals("quitnow"))
					{ SpegnimentoImmediato(); passatoInModalitaRapida = true; }
			}
			catch (InterruptedException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
		}
		server.ConcludiSpegnimento();
		
		if(!passatoInModalitaRapida) System.out.println("\nIl server TURING è stato spento in via ordinaria. Arrivederci alla prossima sessione!");
	}
	
	private void SpegnimentoImmediato()
	{
		System.out.println("Spegnimento rapido in corso...");
		
		int killati = server.pool.getPoolSize();
		
		server.SpegnimentoMorbido();
		int scartati = server.SpegnimentoImmediato();

		try { server.pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS); }
		catch (InterruptedException e) { e.printStackTrace(); }
		
		server.ConcludiSpegnimento();
		
		System.out.printf("\nIl server TURING è stato spento in via rapida (interrotte %d richieste in esecuzione, scartate %d richieste in coda).\nArrivederci alla prossima sessione!", killati, scartati);
	}
}
