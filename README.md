# TURING (disTribUted collaboRative edItiNG)
![Schermata iniziale](https://github.com/alessandro-antonelli/TURING/blob/master/screenshot/client%20inizio.png)

Un'applicazione client-server che realizza uno strumento per l'editing collaborativo di documenti testuali da parte di più utenti (stile Google Documenti), con un insieme di funzioni minimale e interfaccia a riga di comando.

L'implementazione richiede che il server e i client siano in esecuzione sulla stessa macchina, ma è facilmente estendibile per funzionare con macchine diverse collegate via internet (in quanto client e server comunicano già tramite i protocolli TCP, UDP e Java RMI).

Per dettagli sui requisiti richiesti, vedere [__Istruzioni e specifiche__](Istruzioni%20e%20specifiche.pdf); per dettagli sulle scelte implementative vedere la [__Relazione__](Relazione/Relazione.pdf).

Sviluppato come progetto finale del Laboratorio di Programmazione di Reti dell'A.A. 2018/19, tenuto dalla professoressa Laura Ricci con il supporto alla didattica di Andrea Michienzi (facente parte dell'esame di Reti di calcolatori e laboratorio, codice 274AA), nel corso di laurea triennale in Informatica dell'Università di Pisa.

Homepage del corso: https://elearning.di.unipi.it/course/view.php?id=136

## Istruzioni

`` bash session
#download sorgenti e compilazione
git clone https://github.com/alessandro-antonelli/TURING
javac TURING/Server/turing/*.java
javac TURING/Client/turing/*.java

#avvio server
cd TURING/Server/
java turing.MainClass &

#avvio client
cd ../Client
java turing.MainClass
``

## Screenshot

##### Lista dei comandi accettati dal client
![](https://github.com/alessandro-antonelli/TURING/blob/master/screenshot/client%20help.png)

##### Modifica di un documento
![](https://github.com/alessandro-antonelli/TURING/blob/master/screenshot/client%20edit.png)

##### Condivisione di un documento
![](https://github.com/alessandro-antonelli/TURING/blob/master/screenshot/client%20share.png)

##### Chat tra i collaboratori
![](https://github.com/alessandro-antonelli/TURING/blob/master/screenshot/client%20chat.png)

##### Statistiche del server
![](https://github.com/alessandro-antonelli/TURING/blob/master/screenshot/server%20stats.png)
