/*
Università di Pisa - Laboratorio del corso di Reti di Calcolatori - A.A. 2018-2019
TURING: disTribUted collaboRative edItiNG
Codice sviluppato singolarmente da Alessandro Antonelli, matricola 507264
*/

/*
 * La classe rappresenta un documento di TURING nel modo più semplice possibile, ossia con la coppia di stringhe <nomeDocumento, utenteCreatore>.
 * 
 * E' un semplice involucro delle due stringhe e non implementa controlli sulla reale esistenza degli utenti con il nome specificato,
 * né sull'accettabilità o univocità dei nomi dei documenti all'interno dell'istanza di TURING.
 * 
 * I tipi DocumentoConDettagli e DocumentoLatoServer estendono questa classe, aggiungendo ulteriori informazioni sul documento
 * alla rappresentazione fornita da questo tipo.
 PROVA - DA CANCELLARE
 */

package turing.tipidato;

import java.io.Serializable;

/* 
 *		Documento
 * Questa classe definisce il tipo di dato Documento, che rappresenta un documento memorizzato in TURING
 * nel modo più semplice possibile, ossia come coppia di stringhe <nome documento, utente creatore>.
 * 
 * Viene esteso dal tipo DocumentoConDettagli, che completa la rappresentazione con l'insieme dei collaboratori
 * e il numero delle sezioni di cui è costituito.
 */

public class Documento implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final String	nomeDocumento;
	// Volontariamente non modificabile: i documenti non possono essere rinominati!
	
	private final String 	creatore;
	// Volontariamente non modificabile: i documenti non possono cambiare creatore!
	

	public Documento(String nomeDocumento, String creatore)
	{
		this.nomeDocumento = nomeDocumento;
		this.creatore = creatore;
	}
	
	//======= METODI GETTER =======
	
	public String getNome()
	{
		return new String(nomeDocumento);
	}
	
	public String getCreatore()
	{
		return new String(creatore);
	}
}
