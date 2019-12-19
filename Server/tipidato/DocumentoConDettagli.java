/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG
Codice sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

/*
 * La classe rappresenta un documento di TURING in maniera estesa, ossia tramite la quadrupla
 * <nomeDocumento, utenteCreatore, numeroDelleSezioni, insiemeDegliUtentiCollaboratori>.
 * 
 * E' un semplice involucro non implementa controlli sull'accettabilità o univocità dei nomi dei
 * documenti o degli utenti all'interno dell'istanza di TURING.
 * 
 * E' un'estensione del tipo di base Documento, che rappresenta la semplice coppia <nomeDocumento, utenteCreatore>.
 * Il tipo DocumentoLatoServer estende ulteriormente questa classe, aggiungendo ulteriori campi di "book keeping" necessari per
 * la gestione del documento lato server.
 */

package turing.tipidato;

import turing.exceptions.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* 
 *		DocumentoConDettagli
 * Questa classe definisce il tipo di dato DocumentoConDettagli, che rappresenta un documento memorizzato in TURING in modo
 * più completo del tipo Documento (che estende, aggiungendo la lista dei collaboratori e il numero delle sezioni),
 * ma meno del tipo DocumentoLatoServer (che ne è una estensione e aggiunge strutture di book-keeping utili sul lato server).
 */

public class DocumentoConDettagli extends Documento implements Serializable
{
	private static final long serialVersionUID = -2065182558755647605L;

	private HashSet<String>		collaboratori;

	private final int			numSezioni;
	// Volontariamente non modificabile: il numero di sezioni non è modificabile dopo la creazione del documento!
	
	public DocumentoConDettagli(String nomeDocumento, String creatore, int numSezioni)
	{
		super(nomeDocumento, creatore);
		this.numSezioni = numSezioni;
		this.collaboratori = new HashSet<String>();
	}
	
	public DocumentoConDettagli(DocumentoLatoServer doc)
	{
		super(doc.getNome(), doc.getCreatore());
		this.numSezioni = doc.getNumSezioni();
		this.collaboratori = new HashSet<String>(doc.getCollaboratori());
	}
	
	//======= METODI GETTER =======
	
	public Set<String> getCollaboratori()
	{		
		return Collections.unmodifiableSet(collaboratori);
	}
	
	public Boolean isCollaboratore(String utente)
	{
		if(utente == null || utente.isEmpty()) throw new IllegalArgumentException();

		return collaboratori.contains(utente);
	}
	
	public int getNumSezioni()
	{		
		return new Integer(numSezioni);
	}
	
	//======= METODI SETTER =======
	
	public void AggiungiCollaboratore(String utente)
	{
		if(utente == null || utente.isEmpty()) throw new IllegalArgumentException();
		
		if(!isCollaboratore(utente) && !utente.equals(getCreatore())) collaboratori.add(utente);
	}
	
	public void RimuoviCollaboratore(String utente) throws NoSuchUserException
	{
		if(utente == null || utente.isEmpty()) throw new IllegalArgumentException();
		if(!isCollaboratore(utente)) throw new NoSuchUserException("L'utente " + utente + " non fa parte dei collaboratori del documento!");
		
		collaboratori.remove(utente);
	}
}
