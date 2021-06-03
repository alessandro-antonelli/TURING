/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Client
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing;

import turing.exceptions.*;
import turing.tipidato.*;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * 		CLI (Command Line Interface)
 * Questa classe contiene il codice per generare l'output sull'interfaccia a riga di comando, per ricevere l'input da riga di comando,
 * e per decodificare il comando inserito dall'utente (controllando la presenza e la validità dei parametri necessari);
 * ossia il ciclo fetch-decode.
 * L'esecuzione vera e propria del comando viene poi demandata alla classe TuringExecutor.
 * 
 * Si occupa di generare l'interfaccia a linea di comando e di gestire il ciclo "leggi da tastiera/interpreta comando/passa all'esecutore".
 * Questa classe si occupa solamente di riconoscere il comando e di individuare gli argomenti; la validazione degli argomenti e
 * l'esecuzione vera e propria (con il relativo interfacciamento col server) sono contenuti nella classe TuringExecutor. 
 */

public class CLI {
	
	//======= VAR DI ISTANZA =======
	
	private final InoltratoreRichieste inoltratore;
	
	private final ConfigurazioneClient configurazione;
	
	private int ConteggioComandi = 0;
	
	//======= COSTANTI =======
	
	private final String splashScreen =		"      ############   ---------------------------------\n" +
											"      ## TURING ##   disTribUted collaboRative edItiNG\n" +
											"      ############   ---------------------------------\n" +
											"             ::: Client :::\n\n" +
											"Benvenuto! Interagisci digitando un comando. (per l'elenco dei comandi: help)\n" +
											"Per iniziare, registrati o accedi al tuo account.\n";

	private final String usageString =	"I comandi disponibili sono i seguenti:\n\n" +
										"register <username> <password> Registra un nuovo account utente\n" +
										"login <username> <password>    Effettua l'accesso ad un account\n" +
										"logout                         Effettua il logout dall'account\n"+
										"create <doc> <numsezioni>      Crea un nuovo documento\n" +
										"share <doc> <username>         Condivide un documento con un altro utente\n" +
										"show <doc> <sec>               Mostra una sezione di un documento\n" +
										"show <doc>                     Mostra l'intero documento\n" +
										"list                           Mostra la lista dei documenti a cui si ha accesso\n" +
										"edit <doc> <sec>               Inizia la modifica della sezione del documento\n" +
										"end-edit <doc> <sec>           Termina la modifica della sezione del documento\n" +
										"send <msg>                     Invia sulla chat un messaggio senza spazi\n" +
										"send \"<msg con spazi>\"         Invia sulla chat un messaggio con spazi\n" +
										"receive                        Visualizza i messaggi ricevuti sulla chat\n" +
										"stats                          Visualizza statistiche e configurazione del client\n" +
										"quit                           Chiude il client";

	private final String avvisoComandoIncompleto = "Comando incompleto: mancano uno o più parametri obbligatori!\nSintassi:   ";
	
	//======= COSTRUTTORE =======
	
	public CLI(InoltratoreRichieste inoltratore, ConfigurazioneClient configurazione)
	{
		this.inoltratore = inoltratore;
		this.configurazione = configurazione;
		Thread.currentThread().setName("Client-Interfaccia utente");
		System.out.println(splashScreen);
	}
	
	//======= METODO PRINCIPALE =======

	public void CicloFetchDecode()
	{
		Boolean interrompereCiclo = false;
		BufferedReader terminale = new BufferedReader(new InputStreamReader(System.in));
		while(!interrompereCiclo)
		{
			inoltratore.RegistraUsoSessione();
			StampaEventualiNotifiche();
			
			try	{
				System.out.println(); System.err.flush(); System.out.flush();
				String rigaInput = terminale.readLine();
		        if(rigaInput == null || rigaInput.isEmpty()) { throw new NoSuchElementException(); }
	
		        StringTokenizer tokenizerRigaInput = new StringTokenizer(rigaInput);
		        final String comando = tokenizerRigaInput.nextToken();
		        	
		        switch (comando)
		        {
		        	case "register":
	        		{
	        			DecodeRegister(tokenizerRigaInput);	        			
	        			ConteggioComandi++; continue;
	        		}
		        	case "login":
	        		{
	        			DecodeLogin(tokenizerRigaInput);
	        			ConteggioComandi++; continue;
	        		}
		        	case "logout":
	        		{
	        			DecodeLogout();
	        			ConteggioComandi++; continue;
	        		}
		        	case "create":
	        		{
	        			DecodeCreate(tokenizerRigaInput);
	        			ConteggioComandi++; continue;
	        		}
		        	case "share":
	        		{
	        			DecodeShare(tokenizerRigaInput);
	        			ConteggioComandi++; continue;
	        		}
		        	case "show":
	        		{
	        			DecodeShow(tokenizerRigaInput);	        			
	        			ConteggioComandi++; continue;
	        		}
		        	case "list":
	        		{
	        			DecodeList();	        			
	        			ConteggioComandi++; continue;
	        		}
		        	case "edit":
	        		{
	        			DecodeEdit(tokenizerRigaInput);
	        			ConteggioComandi++; continue;
	        		}
		        	case "end-edit":
	        		{
	        			DecodeEndEdit(tokenizerRigaInput);
	        			ConteggioComandi++; continue;
	        		}
		        	case "send":
	        		{	        			
	        			DecodeSend(rigaInput, tokenizerRigaInput);
	        			ConteggioComandi++; continue;
	        		}
		        	case "receive":
	        		{
	        			DecodeReceive();
	        			ConteggioComandi++; continue;
	        		}
		        	case "help":
	        		{
	        			System.out.println(usageString);
	        			continue;
	        		}
		        	case "stats":
		        	{
		        		StampaStatistiche();
		        		continue;
		        	}
		        	case "quit":
		        	{
		        		interrompereCiclo = true;
		        		continue;
		        	}
		        	default:
		        	{
		        		System.err.println("Comando \"" + comando + "\" inesistente\nPer l'elenco dei comandi digita: help");
		        		continue;
		        	}
		        }
			}
			catch (NoSuchElementException e) // Sollevata quando rigaInput non contiene alcun token (non si è digitato il nome del comando)
			{
				System.err.println("Comando non valido\nPer l'elenco dei comandi digita: help");
				continue;
			}
			catch (java.net.ConnectException e)
			{
				ErroreFatale("il server non è in ascolto delle richieste di connessione", "Controlla che il server sia "+
				"stato effettivamente  avviato su questo stesso host e che sia in ascolto sulla porta " + configurazione.PORTAPRINCIPALE, null);
			}
			catch (ClassNotFoundException e) // Errore di serializzazione nella comunicazione client-server
			{
				ErroreFatale("file di alcune classi mancante o non aggiornato", "Il client non riesce a reperire i file di alcune classi "+
				"che definiscono i tipi di dato scambiati con il server tramite serializzazione, oppure è dotato di una copia vecchia", e);
			}
			catch (BindException e)
			{
				ErroreFatale("una delle porte utilizzate è in uso da un'altra applicazione oppure è riservata", "Usa gli argomenti a linea di comando per specificare manualmente delle porte diverse", e);
			}
			catch (java.net.SocketException | EOFException e)
			{
				ErroreFatale("il server si è disconnesso inaspettatamente", null, e);
			}
			catch (IOException e) // Sollevata dalla readLine e da alcuni metodi
			{
				ErroreFatale("si è verificato un errore di Input/Output", null, e);
			}
		}
		try {
			inoltratore.TerminaThreadChat();
			terminale.close();
			
			// Svuoto la cartella dell'istanza, con i file scaricati
			File cartellaIstanza = new File(configurazione.FILEBASEPATH);
			if(cartellaIstanza.exists()) inoltratore.CancellaCartella(cartellaIstanza);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	//======= METODI AUSILIARI =======
	
	void ErroreFatale(String titolo, String suggerimentoSoluzione, Exception e)
	{
		System.err.println("Errore fatale: " + titolo + "!");
		if(suggerimentoSoluzione != null) System.err.println(suggerimentoSoluzione + ".");
		System.err.println("Il client è stato terminato.");

		if(e != null)
		{
			System.err.println("\nSegue lo stack delle invocazioni:");
			e.printStackTrace();
		}
		System.exit(1);
	}
	
	void StampaRispostaNegativa(EccezioneTuring e)
	{
		System.err.printf(e.getMessage());
		if(e.getCause() != null)
			System.err.println(e.getCause().getMessage());
		else System.err.println();
	}

	/*
	 ************* SEGUONO LE PROCEDURE PER LA DECODIFICA DI CIASCUN COMANDO ************
	*/
	
	private void DecodeRegister(StringTokenizer tokenizerRigaInput)
	{
		try {
			final String username = tokenizerRigaInput.nextToken();
			final String password = tokenizerRigaInput.nextToken();

			inoltratore.RegistraUtente(username, password);
			System.out.printf("Benvenuto/a, %s! La registrazione del tuo account è stata eseguita con successo!\nPer usarlo, ricordati di eseguire l'accesso.\n", username);
		}
		catch (NoSuchElementException e) //Manca almeno un parametro
			{ System.err.println(avvisoComandoIncompleto + "register <username> <password>"); }
		catch (IllegalArgumentException e) { System.err.println(e.getMessage()); }
		catch (AlreadyLoggedException e) { System.err.println(e.getMessage()); }
		catch (UnavailableNameException e) { System.err.println(e.getMessage()); }
		catch (java.rmi.ConnectException e) { ErroreFatale("il server non è in ascolto delle richieste di registrazione via RMI", "Controlla che il server sia stato effettivamente avviato su questo stesso host", e); }
		catch (RemoteException e) { ErroreFatale("comunicazione RMI con il server non riuscita: " + e.getMessage(), null, e); }
		catch (NotBoundException e) { ErroreFatale("l'oggetto remoto CreatoreAccount non è presente nel registry RMI", e.getMessage(), e); }
	}
	
	private void DecodeLogin(StringTokenizer tokenizerRigaInput) throws ClassNotFoundException
	{
		try {
			final String username = tokenizerRigaInput.nextToken();
			final String password = tokenizerRigaInput.nextToken();

			inoltratore.AccediAccount(username, password);
			System.out.printf("Bentornato/a, %s! Accesso al tuo account eseguito correttamente!\n", username);
		}
		catch (NoSuchElementException e) //Manca almeno un parametro
			{ System.err.println(avvisoComandoIncompleto + "login <username> <password>"); }
		catch (IOException e) { // Il server ha rifiutato la connessione!
			ErroreFatale("il server non è in ascolto delle richieste di login, oppure ha rifiutato la richiesta",
					"Controlla che il server sia effettivamente in esecuzione su questo stesso host e che sia in ascolto sulla porta "
					+ configurazione.PORTAPRINCIPALE, e);
		}
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeLogout() throws IOException, ClassNotFoundException
	{
		try {
			String username = inoltratore.EsciAccount();
			System.out.println("Logout eseguito con successo. Arrivederci, " + username + "!");
		}
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeCreate(StringTokenizer tokenizerRigaInput) throws ClassNotFoundException, IOException
	{
		try
		{
			final String docname = tokenizerRigaInput.nextToken();
			final int numSezioni = Integer.parseInt(tokenizerRigaInput.nextToken());
			
			inoltratore.CreaDocumento(docname, numSezioni);
			
			System.out.printf("Eseguito con successo!\nE' stato creato il documento \"%s\", composto di %d sezioni\n", docname, numSezioni);
		}
		catch (NumberFormatException e)
			{ System.err.println("Impossibile eseguire: il parametro numsezioni deve essere un numero intero!"); }
		catch (NoSuchElementException e) //Manca almeno un parametro
			{ System.err.println(avvisoComandoIncompleto + "create <doc> <numsezioni>"); }
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeShare(StringTokenizer tokenizerRigaInput) throws ClassNotFoundException, IOException
	{
		try
		{
			final String docname = tokenizerRigaInput.nextToken();
			final String username = tokenizerRigaInput.nextToken();
			
			inoltratore.CondividiDocumento(docname, username);
			
			System.out.printf("Eseguito con successo!\nIl documento \"%s\" è stato condiviso con l'utente %s!\n", docname, username);
		}
		catch (NoSuchElementException e) //Manca almeno un parametro
			{ System.err.println(avvisoComandoIncompleto + "share <doc> <username>"); }
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeShow(StringTokenizer tokenizerRigaInput) throws ClassNotFoundException, IOException
	{
		try
		{
			final String docname = tokenizerRigaInput.nextToken();
			
			if(tokenizerRigaInput.hasMoreTokens())
			{
				// Versione a due parametri: visualizzazione di una sola sezione
				final int numSezione = Integer.parseInt(tokenizerRigaInput.nextToken());
				Boolean InCorsoDiModifica = inoltratore.VisualizzaSezione(docname, numSezione);
				
				System.out.printf("Eseguito con successo!\nLa sezione %d del documento '%s' è stata scaricata.\nLa trovi al percorso: %s\n", numSezione, docname, inoltratore.PercorsoFileSystem(docname, numSezione));
				
				if(InCorsoDiModifica) System.out.println("Un altro utente la sta modificando proprio in questo momento.");
				else System.out.println("Al momento nessun utente la sta modificando.");
			} else
			{
				// Versione ad un parametro: visualizzazione dell'intero documento
				Integer[] elencoSezioniInCorsoDiModifica = inoltratore.VisualizzaInteroDocumento(docname);
				
				System.out.printf("Eseguito con successo!\nIl documento '%s' è stato scaricato per intero.\nLo trovi al percorso: %s\n", docname, inoltratore.PercorsoFileSystem(docname, -1) + "DocumentoCompleto.txt");
				
				if(elencoSezioniInCorsoDiModifica.length > 0)
				{
					if(elencoSezioniInCorsoDiModifica.length == 1) System.out.printf("Al momento, è in corso una modifica sulla sezione numero ");
					else System.out.printf("Al momento, sono in corso modifiche sulle sezioni numero ");
					
					for(Integer numSez : elencoSezioniInCorsoDiModifica) System.out.printf("%d; ", numSez);
					
					if(elencoSezioniInCorsoDiModifica.length == 1) System.out.println("la versione scaricata ovviamente non include tale modifica.");
					else System.out.println("la versione scaricata ovviamente non include tali modifiche.");
				} else System.out.println("Al momento, su nessuna sezione sono in corso modifiche.");
			}
		}
		catch (NoSuchElementException e) //Manca il parametro obbligatorio
			{ System.err.println(avvisoComandoIncompleto + "show <doc>   [oppure]   show <doc> <sec>"); }
		catch (NumberFormatException e)
			{ System.err.println("Impossibile eseguire: il parametro sec deve essere un numero intero!"); }
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeList() throws ClassNotFoundException, IOException
	{
		try
		{
			DocumentoConDettagli[] elenco = inoltratore.ElencaDocumenti();

			if(elenco.length == 0)
				System.out.println("Non ci sono documenti che tu abbia creato o di cui tu abbia ricevuto l'invito.");
			else
			{
				if(elenco.length == 1) System.out.printf("Hai accesso a un documento. Questi i suoi dettagli:\n\n");
				else System.out.printf("Hai accesso a %d documenti. Questa è la lista:\n\n", elenco.length);
				
				for(DocumentoConDettagli doc : elenco)
				{
					System.out.printf("%s\n  Creatore........ %s\n  Sezioni......... %d\n  Collaboratori... ", doc.getNome(), doc.getCreatore(), doc.getNumSezioni());

					if(doc.getCollaboratori().isEmpty())
						System.out.printf("nessuno");
					else
					{
						if(doc.getCollaboratori().size() == 1) System.out.printf("un utente: ");
						else System.out.printf("%d utenti: ", doc.getCollaboratori().size());
						
    					Iterator<String> iterCollaboratori = doc.getCollaboratori().iterator();
    					while(iterCollaboratori.hasNext())
    					{
    						System.out.printf(iterCollaboratori.next());
    						if(iterCollaboratori.hasNext()) System.out.printf(", ");
    					}
					}
					System.out.printf("\n\n");
				}
			}
		}
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeEdit(StringTokenizer tokenizerRigaInput) throws ClassNotFoundException, BindException, IOException
	{
		try
		{
			final String docname = tokenizerRigaInput.nextToken();
			final int numSezione = Integer.parseInt(tokenizerRigaInput.nextToken());
			
			inoltratore.IniziaModifica(docname, numSezione);
			
			System.out.printf("Eseguito con successo!\nLa sezione %d del documento \"%s\" è stata scaricata!\nLa trovi al percorso %s\nEsegui pure la modifica con il tuo editor di testo preferito;\nquando hai finito usa il comando 'end-edit' per salvare la tua versione sul server.\n", numSezione, docname, inoltratore.PercorsoFileSystem(docname, numSezione));
		}
		catch (NoSuchElementException e) //Mancano uno o più parametri
			{ System.err.println(avvisoComandoIncompleto + "edit <doc> <sec>"); }
		catch (NumberFormatException e)
			{ System.err.println("Impossibile eseguire: il parametro sec deve essere un numero intero!"); }
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeEndEdit(StringTokenizer tokenizerRigaInput) throws ClassNotFoundException, IOException
	{
		try
		{
			final String docname = tokenizerRigaInput.nextToken();
			final int numSezione = Integer.parseInt(tokenizerRigaInput.nextToken());
			
			inoltratore.TerminaModifica(docname, numSezione);
			
			System.out.printf("Eseguito con successo!\nLa sezione %d del documento \"%s\" è stata aggiornata!\n", numSezione, docname);
		}
		catch (NoSuchElementException e) //Mancano uno o più parametri
			{ System.err.println(avvisoComandoIncompleto + "end-edit <doc> <sec>"); }
		catch (NumberFormatException e)
			{ System.err.println("Impossibile eseguire: il parametro sec deve essere un numero intero!"); }
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeSend(String rigaInput, StringTokenizer tokenizerRigaInput) throws IOException
	{
		try
		{
			final String messaggio;
			int inizioParametro = rigaInput.indexOf("send ") + 5;
			
			if(rigaInput.charAt(inizioParametro) != '"')
			{
				//Non sono state usate le virgolette per delimitare il parametro messaggio
				messaggio = tokenizerRigaInput.nextToken();
			}
			else
			{
				//Sono state usate le virgolette per delimitare il parametro messaggio
				inizioParametro++;
				int fineParametro = rigaInput.indexOf("\"", inizioParametro);

				//segnala il caso in cui l'utente inserisce un parametro vuoto delimitato da virgolette ("")
				if(inizioParametro == fineParametro) throw new NoSuchElementException();
				
				messaggio = rigaInput.substring(inizioParametro, fineParametro);
			}
			
			
			inoltratore.InviaMessaggio(messaggio);
			
			System.out.println("Messaggio inviato sulla chat.");
		}
		catch (IndexOutOfBoundsException | NoSuchElementException e) //Manca il parametro
			{ System.err.println(avvisoComandoIncompleto + "send <messaggio>  *oppure*  send \"<messaggio con spazi>\""); }
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void DecodeReceive()
	{
		try
		{
			Queue<MessaggioChat> listaMessaggi = inoltratore.RiceviMessaggi();

			if(listaMessaggi == null)
				System.out.println("Non hai ricevuto nuovi messaggi.");
			else
			{
				if(listaMessaggi.size() == 1) System.out.printf("Hai ricevuto un nuovo messaggio:\n\n");
				else System.out.printf("Hai ricevuto %d nuovi messaggi:\n\n", listaMessaggi.size());
				
				int maxUsernameLength = 0;
				for(MessaggioChat msg : listaMessaggi)
					if(msg.getMittente().length() > maxUsernameLength) maxUsernameLength = msg.getMittente().length();
				
				do
				{
					MessaggioChat msg = listaMessaggi.remove();
					
					int lunghezzaPadding = maxUsernameLength - msg.getMittente().length();
					String padding = "";
					for(int i=0; i<lunghezzaPadding; i++) padding += " ";
					
					System.out.println(msg.getOrario().toString() + " " + msg.getMittente() + padding + ": " + msg.getMessaggio());
					
				} while(!listaMessaggi.isEmpty());
			}
		}
		catch (EccezioneTuring e) { StampaRispostaNegativa(e); }
	}
	
	private void StampaEventualiNotifiche()
	{
		Vector<Documento> condivisioniPendenti = inoltratore.getNotifichePendenti();
		
		if(condivisioniPendenti != null && !condivisioniPendenti.isEmpty())
		{
			if(condivisioniPendenti.size() == 1) System.out.printf("\n===> \u00C8 arrivata una nuova notifica! <===\n");
			else System.out.printf("\n===> Sono arrivate %d nuove notifiche! <===\n", condivisioniPendenti.size());
			
			for(Documento documento : condivisioniPendenti)
				System.out.printf("* L'utente %s ha condiviso con te il documento '%s'\n", documento.getCreatore(), documento.getNome());
			
			if(condivisioniPendenti.size() == 1)
				System.out.println("\nOra sei tra i collaboratori del documento, e puoi contribuire a modificarlo!");
			else
				System.out.println("\nOra sei tra i collaboratori dei documenti, e puoi contribuire a modificarli!");
		}
	}
	
	private void StampaStatistiche()
	{
		System.out.println("      [Statistiche e configurazione del client]");
		System.out.println();
		System.out.println("Numero di comandi ricevuti e inoltrati al server... " + ConteggioComandi);
		System.out.println();
		
		System.out.println("Cartella per il salvataggio dei documenti.. " + configurazione.FILEBASEPATH);
		System.out.println("Porta per la comunicazione col server...... " + configurazione.PORTAPRINCIPALE);
		System.out.println("Porta per la ricezione di notifiche........ " + configurazione.PORTANOTIFICHE);
		System.out.println("Porta per lo scambio di messaggi........... " + configurazione.PORTAMESSAGGI);
		System.out.println("Massima lunghezza ammessa per i messaggi... " + configurazione.MAXMSGLENGTH + " caratteri");
		System.out.println(); //A capo finale per distanziare da comandi successivi
	}
}
