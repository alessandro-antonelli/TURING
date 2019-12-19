/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Client
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing;

import java.io.File;

/*
 * 		ConfigurazioneClient
 * Questa classe costituisce il luogo centrale per la memorizzazione di tutte
 * le costanti necessarie per l'esecuzione del server (fissate all'avvio e
 * non modificate successivamente).
 */

public class ConfigurazioneClient
{
	//======= VAR DI ISTANZA =======
	
	public final String nomePubblicoServCreatoreAccount = "CreatoreAccountTuring";
	// Nome pubblico con cui il servizio di creazione degli account è memorizzato nel registry RMI
	
	public String FILEBASEPATH = System.getProperty("user.dir") + File.separator + "DocumentiScaricati" + File.separator;
	// Percorso della cartella del file system dove vengono salvati i documenti durante la loro visualizzazione o modifica
	// (la cartella è relativa alla singola istanza del client)
	
	public File FileSegnadataIstanza;
	// File contenuto nella cartella FILEBASEPATH che ha lo scopo di indicare l'ultima data di uso della cartella stessa
	// (tramite l'attributo "ultima modifica" del file system). Serve per poter cancellare la cartella quando l'istanza non la usa da tempo.
	
	public final int PORTAPRINCIPALE;
	// Porta usata per la connessione al welcome socket del server e l'invio di richieste
	
	public final int PORTANOTIFICHE;
	// Porta usata per ricevere le notifiche degli inviti (share) ricevuti
	
	public final int PORTAMESSAGGI;
	// Porta usata per l'invio e la ricezione di messaggi sulla chat

	public final int MAXMSGLENGTH = 4096;
	// Massima lunghezza dei messaggi scambiabili sulla chat (in byte)

	//======= COSTRUTTORE =======
	
	public ConfigurazioneClient(int PORTAPRINCIPALE, int PORTANOTIFICHE, int PORTAMESSAGGI)
	{
		if(PORTAPRINCIPALE < 0)	this.PORTAPRINCIPALE = 41318; //default
						else	this.PORTAPRINCIPALE = PORTAPRINCIPALE;
		
		if(PORTANOTIFICHE < 0)	this.PORTANOTIFICHE = 41319; //default
						else	this.PORTANOTIFICHE = PORTANOTIFICHE;

		if(PORTAMESSAGGI < 0)	this.PORTAMESSAGGI = 45687; //default
						else	this.PORTAMESSAGGI = PORTAMESSAGGI;
	}
}
