/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.unice.miage.sd.tinydfs.nodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stéphane Kuma
 */
public class ImplementMaster extends UnicastRemoteObject implements Master {
    private String dossierRacineDfs;
    private int nbrEsclave;
    private Slave[] esclave;
    private Slave esclaveDroit;
    private Slave esclaveGauche;
    private boolean estContruit;
    
    private HashMap<String, List<Thread>> fichierVerrouille;

    public ImplementMaster(String dossierRacineDfs, int nbrEsclave) throws RemoteException, Exception {
        // Constructeur du RMI
        super();
        
        // Vérification du nombre du nombre d'esclave
        if((nbrEsclave + 2 & nbrEsclave + 1) != 0) {
           throw new Exception("Le nombre d'esclave: " + nbrEsclave + " n'est pas un multiple de deux");
        }
        
        // Initialisation et création du dossier racine dfs si celui ci n'existaot pas
        this.dossierRacineDfs = dossierRacineDfs;
        File dossierRacineFichierDfs = new File(dossierRacineDfs);
        if (!dossierRacineFichierDfs.exists()) {
            dossierRacineFichierDfs.mkdir();
            System.out.println("Création du dossier:" + dossierRacineFichierDfs.getName());
        }
        
        // Initialisation des autres champs
        this.nbrEsclave = nbrEsclave;
        this.estContruit = false;
        this.esclave = new Slave[nbrEsclave];
        this.esclaveDroit = null;
        this.esclaveGauche = null;
        this.fichierVerrouille = new HashMap<>();
    }

    /***
     * renvoie le dossier racine dans le quel la macine maître écrit
     * @return String dossierRacineDfs
     * @throws java.rmi.RemoteException
     */
    @Override
    public String getDfsRootFolder() throws RemoteException {
        return this.dossierRacineDfs;
    }

    /***
     * renvoie le nombre de machine esclave à enregistrer
     * @return int nbrEsclave
     * @throws java.rmi.RemoteException
     */
    @Override
    public int getNbSlaves() throws RemoteException {
        return this.nbrEsclave;
    }

    /***
     * enregistre le fichier sur le système de fichier distribué en invoquant la méthode saveBytes
     * @param file
     * @throws RemoteException 
     */
    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void saveFile(File file) throws RemoteException {
        // Lecture du fichier
        try {
            saveBytes(file.getName(), Files.readAllBytes(file.toPath()));
        }
        catch(IOException e) {
            // Si une erreur survient, afficher la pile d'erreurs
            e.printStackTrace();
        }
    }
    
    /***
     * Construit l'arbre binaire de notre système de fichier
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private void construireArbreBinaire() //throws UnknownHostException
    {
        // référencement des machines esclaves
        for (int i = 0; i < this.nbrEsclave; i++) {
            try {
                // on récupère le nom du service proposé par une machine esclave
                String chemin = "rmi://" + InetAddress.getLocalHost().getHostAddress() + "/esclave" + i;
                // on accède au service
                Remote service = Naming.lookup(chemin);
                // on l'enregistre dans le tableau dans notre tableau d'eclave
                this.esclave[i] = (Slave) service;
            } catch (UnknownHostException | NotBoundException | MalformedURLException | RemoteException e) {
                // si une erreur survient, affiche lapile d'erreur
                e.printStackTrace();
            } /*catch (NotBoundException ex) {
                Logger.getLogger(ImplementMaster.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(ImplementMaster.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RemoteException ex) {
                Logger.getLogger(ImplementMaster.class.getName()).log(Level.SEVERE, null, ex);
            }*/
        }
        
        // Initialisation des noeuds fils (machines esclaves) de la machine maître
        this.esclaveGauche = this.esclave[0];
        this.esclaveDroit = this.esclave[1];
        
        // construction de l'arbre binaire
        int i = 1;
        while(((i + 1) * 2) - 2 < this.esclave.length) {
            try {
               this.esclave[i - 1].setLeftSlave(this.esclave[((i + 1) * 2) - 2]);
               this.esclave[i - 1].setRightSlave(this.esclave[((i + 1) * 2) -1]);
               i++;
            } catch (RemoteException e) {
                // si une erreur survient, affiche lapile d'erreur
                e.printStackTrace();
            }
        }
        
        // vérification des noeuds fils(machines esclaves) des machines esclaves(ici maître)
        for (int j = 0; j < (this.esclave.length / 2) - 1; j++) {
            try {
                System.out.println("Machine esclave" + this.esclave[j].getId() + " a pour machine "
                        + "esclave gauche la machine esclave" + this.esclave[j].getLeftSlave().getId()
                        + " et a pour machine esclave droit l'esclave la machine esclave"
                        + this.esclave[j].getRightSlave().getId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    
    /***
     * décompose un tableau de byte en nbrEsclave tableau de byte
     * Renvoit une liste contenant tout les tableaux obtenus
     * @param contenuFichier
     * @return 
     */
    private List<byte[]> obtenirTableauxDeBytes(byte[] contenuFichier) {
        System.out.println("Longueur du contenu du fichier : " + contenuFichier.length);
        List<byte[]> octets = new ArrayList<>();
        // calcul du nombre de tableau à créer
        int tailleTableauDeByte = contenuFichier.length / this.nbrEsclave;
        // calcul du nombre d'octets(bytes) en trop
        int bytePasAPorte = contenuFichier.length % this.nbrEsclave;
        int compteur = 0;
        
        // création des tableaux
        for (int i = 0; i < this.nbrEsclave; i++) {
            byte[] esclave;
            if(bytePasAPorte == 0) {
                esclave = new byte[tailleTableauDeByte];
            }
            else {
                esclave = new byte[tailleTableauDeByte + 1];
                bytePasAPorte--;
            }
            // Remplissage du tableau
            for (int j = 0; j < esclave.length; j++) {
                esclave[j] = contenuFichier[compteur];
                compteur++;
            }
            octets.add(esclave);
        }
        return octets;
    }
    
    /***
     * sauvegarde un tableau de bytes(octets) dans les machines esclaves
     * @param filename
     * @param fileContent
     * @throws RemoteException 
     */
    @Override
    public void saveBytes(final String filename, final byte[] fileContent) throws RemoteException {
        // Si c'est le premier appel du client, on construit l'arbre
        if(!this.estContruit) {
            this.construireArbreBinaire();
            this.estContruit = true;
        }
        
        // création d'un thread pour éviter la sauvegarde bloquante
        Thread fil;
        fil = new Thread(new Runnable() {
            @Override
            @SuppressWarnings({"element-type-mismatch", "CallToPrintStackTrace"})
            public void run() {
                System.out.println("Fil en cours d'éxécution");
                //On découpe notre tableau en nbSlave tableau de taille égal
                List<byte[]> fichierDivise = obtenirTableauxDeBytes(fileContent);
                //On découpe la liste en deux pour chacun des slaves fils du master
                List<byte[]> pesclaveGauche = new ArrayList<>(fichierDivise.subList(0, fichierDivise.size() / 2));
                List<byte[]> pesclaveDroit = new ArrayList<>(fichierDivise.subList(fichierDivise.size() / 2, fichierDivise.size()));
                try {
                    //Sauvegarde des données dans les slaves du master
                    esclaveGauche.subSave(filename, pesclaveGauche);
                    esclaveDroit.subSave(filename, pesclaveDroit);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                /* Une fois le thread terminé, il se retire
                * lui même de la liste, afin de retirer le lock
                */
                fichierVerrouille.get(filename).remove(this);
            }
        });
        // Sauvegarde des threads dans la liste correspondant à la Clé juste avant le décelnchement du Thread
        if (!this.fichierVerrouille.containsKey(filename)) {
                this.fichierVerrouille.put(filename, new ArrayList<Thread>());
        }
        this.fichierVerrouille.get(filename).add(fil);
        //On démarre le thread
        fil.start();
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public File retrieveFile(String filename) throws RemoteException {
        // On récupère les morceaux de fichiers contenus dans les machines esclaves
        byte[] b = retrieveBytes(filename);
        // on retourne null si rien n'a été récupéré
        if(b==null) {
            return null;
        }
        //On crée le fichier qui sera envoyé à l'utilisateur
        File fichier = new File(this.dossierRacineDfs + File.separator + filename);
        try {
            if (!fichier.exists()) {
                fichier.createNewFile();
            } else {
                fichier.delete();
                fichier.createNewFile();
            }
            try ( //On écrit les données dans ce fichier
                    FileOutputStream fluxDeSortie = new FileOutputStream(fichier)) {
                fluxDeSortie.write(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //On renvoit le fichier
        return fichier;
    }

    @Override
    public byte[] retrieveBytes(String filename) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
