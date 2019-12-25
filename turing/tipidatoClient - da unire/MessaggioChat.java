/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG
Codice sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

package turing.tipidato;

import java.io.Serializable;
import java.sql.Time;

/* 
 *		MessaggioChat
 * Rappresenta i messaggi scambiati sulla chat come tripla <orario invio, utente mittente, testo messaggio>.
 */

public class MessaggioChat implements Serializable {

private static final long serialVersionUID = 2158495649840864870L;
	
	private Time oraInvio;
	private String mittente;
	private String messaggio;
	
	public MessaggioChat(Time orario, String mittente, String messaggio)
	{
		this.messaggio = messaggio;
		this.oraInvio = orario;
		this.mittente = mittente;
	}
	
	public String getMessaggio()
	{
		return new String(messaggio);
	}
	
	public Time getOrario()
	{
		return oraInvio;
	}
	
	public String getMittente()
	{
		return new String(mittente);
	}
}
