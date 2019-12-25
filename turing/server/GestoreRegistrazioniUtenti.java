/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.server;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import turing.exceptions.*;
import turing.tipidato.UtenteLatoServer;

/* 
 *		GestoreRegistrazioniUtenti
 * Questa classe costituisce la classe implementazione del servizio remoto RMI specificato dall'interfaccia CreatoreAccount.
 */

public class GestoreRegistrazioniUtenti extends RemoteServer implements CreatoreAccount
{
	//======= VAR DI ISTANZA =======
	
	private static final long serialVersionUID = -4251911749372076823L;
	private final transient TuringServer server;
	
	//======= COSTRUTTORE =======

	public GestoreRegistrazioniUtenti(TuringServer server, ConfigurazioneServer config) throws RemoteException
	{
		this.server = server;
		
		//ESEGUO EXPORT E BIND
		CreatoreAccount stub = (CreatoreAccount) UnicastRemoteObject.exportObject(this, 0);
		Registry reg = LocateRegistry.createRegistry(1099); //Crea il registry sul localhost e sulla porta di default dei registry (1099)
		
		// Verifico che non ci sia già un servizio associato allo stesso nome pubblico (residuo di esecuzioni precedenti),
		// ed eventualmente elimino l'associazione dal registry 
		for(String nomeServizio: reg.list())
			if(nomeServizio.equals(config.nomePubblicoServCreatoreAccount))
			{
				try { reg.unbind(config.nomePubblicoServCreatoreAccount); }
				catch (NotBoundException e1) { e1.printStackTrace(); } //Non si verificherà
				break;
			}
		
		// Eseguo nel registry l'associazione del servizio al nome pubblico
		try { reg.bind(config.nomePubblicoServCreatoreAccount, stub); }
		catch (AlreadyBoundException e) { e.printStackTrace(); } //Non si verificherà (ho controllato sopra)
	}
	
	//======= IMPLEMENTAZIONE DEL SERVIZIO =======

	@Override
	public void RegistraUtente(String username, String password)
			throws RemoteException, AlreadyLoggedException, IllegalArgumentException, UnavailableNameException
	{
		// Parametri vuoti
		if(username == null || password == null) throw new IllegalArgumentException();
		if(username.isEmpty() || password.isEmpty()) throw new IllegalArgumentException();

		// Username già occupato
		if(server.EsisteUtente(username) == true)
			throw new UnavailableNameException("Impossibile eseguire la registrazione:\nIl nome utente " + username + " non è disponibile (è già assegnato a un altro utente)!");
		
		// Tutto ok, effettuo registrazione
		UtenteLatoServer profilo;
		profilo = new UtenteLatoServer(username, password);
		server.aggiungiUtente(profilo);
	}

}
