/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG - Server
Programma sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import turing.exceptions.*;

/* 
 *		CreatoreAccount
 * Questo file definisce l'interfaccia remota del servizio RMI che gestisce la registrazione di nuovi account utente nel server,
 * che deve essere esportata nel registry e implementata da stub nel server e nel client.
 */

public interface CreatoreAccount extends Remote {
	
	public void RegistraUtente(String username, String password)
			throws RemoteException, AlreadyLoggedException, IllegalArgumentException, UnavailableNameException;

}
