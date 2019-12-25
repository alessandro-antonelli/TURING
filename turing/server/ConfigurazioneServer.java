/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.server;

import java.io.File;

/* 
 *		ConfigurazioneServer
 * Questa classe costituisce il Luogo centrale per la memorizzazione di tutte
 * le costanti necessarie per l'esecuzione del server (fissate all'avvio
 * e non modificate successivamente)
 */

public class ConfigurazioneServer
{
	//======= VAR DI ISTANZA =======
	
	public final String nomePubblicoServCreatoreAccount = "CreatoreAccountTuring";
	// Nome pubblico con cui il servizio di creazione degli account è memorizzato nel registry RMI
	
	public final String FILEBASEPATH = System.getProperty("user.dir") + File.separator + "DatabaseDocumenti" + File.separator;
	// Percorso della cartella del file system dove vengono memorizzati i documenti creati dagli utenti
	
	public final int PORTAPRINCIPALE;
	//usata per la connessione al welcome socket del server e la ricezione di richieste
	
	public final int PORTANOTIFICHE;
	//usata per recapitare al client le notifiche degli inviti (share) ricevuti
	
	public final int CodaWelcomeSock = 10;
	
	//======= COSTRUTTORE =======
	
	public ConfigurazioneServer(int PORTAPRINCIPALE, int PORTANOTIFICHE)
	{
		if(PORTAPRINCIPALE < 0)	this.PORTAPRINCIPALE = 41318; //default
						else	this.PORTAPRINCIPALE = PORTAPRINCIPALE;

		if(PORTANOTIFICHE < 0)	this.PORTANOTIFICHE = 41319; //default
						else	this.PORTANOTIFICHE = PORTANOTIFICHE;
	}
}
