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

/**
 *
 * @author Stéphane Kuma
 */
public class ImplementMaster extends UnicastRemoteObject implements Master, FileSizeInterface {
    private String dfsRootFolder;
    private int slaveNb;
    private Slave[] slave;
    private Slave leftSlave;
    private Slave rightSlave;
    private boolean isBuild;
    
    private HashMap<String, List<Thread>> filelocked;

    public ImplementMaster(String dfsRootFolder, int slaveNb) throws RemoteException, Exception {
        // Constructeur du RMI
        super();
        
        // Vérification du nombre du nombre d'esclave
        if((slaveNb + 2 & slaveNb + 1) != 0) {
           throw new Exception("Le nombre d'esclave: " + slaveNb + " n'est pas un multiple de deux");
        }
        
        // Initialisation et création du dossier racine dfs si celui ci n'existaot pas
        this.dfsRootFolder = dfsRootFolder;
        File dfsRootFileFolder = new File(dfsRootFolder);
        if (!dfsRootFileFolder.exists()) {
            dfsRootFileFolder.mkdir();
            System.out.println("Création du dossier:" + dfsRootFileFolder.getName());
        }
        
        // Initialisation des autres champs
        this.slaveNb = slaveNb;
        this.isBuild = false;
        this.slave = new Slave[slaveNb];
        this.rightSlave = null;
        this.leftSlave = null;
        this.filelocked = new HashMap<>();
    }

    /***
     * renvoie le dossier racine dans le quel la macine maître écrit
     * @return String dfsRootFolder
     * @throws java.rmi.RemoteException
     */
    @Override
    public String getDfsRootFolder() throws RemoteException {
        return this.dfsRootFolder;
    }

    /***
     * renvoie le nombre de machine esclave à enregistrer
     * @return int slaveNb
     * @throws java.rmi.RemoteException
     */
    @Override
    public int getNbSlaves() throws RemoteException {
        return this.slaveNb;
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
        for (int i = 0; i < this.slaveNb; i++) {
            try {
                // on récupère le nom du service proposé par une machine esclave
                String path = "rmi://" + InetAddress.getLocalHost().getHostAddress() + "/esclave" + i;
                // on accède au service
                Remote remote = Naming.lookup(path);
                // on l'enregistre dans le tableau dans notre tableau d'eclave
                this.slave[i] = (Slave) remote;
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
        this.leftSlave = this.slave[0];
        this.rightSlave = this.slave[1];
        
        // construction de l'arbre binaire
        int i = 1;
        while(((i + 1) * 2) - 2 < this.slave.length) {
            try {
               this.slave[i - 1].setLeftSlave(this.slave[((i + 1) * 2) - 2]);
               this.slave[i - 1].setRightSlave(this.slave[((i + 1) * 2) -1]);
               i++;
            } catch (RemoteException e) {
                // si une erreur survient, affiche lapile d'erreur
                e.printStackTrace();
            }
        }
        
        // vérification des noeuds fils(machines esclaves) des machines esclaves(ici maître)
        for (int j = 0; j < (this.slave.length / 2) - 1; j++) {
            try {
                System.out.println("Machine esclave" + this.slave[j].getId() + " a pour machine "
                        + "esclave gauche la machine esclave" + this.slave[j].getLeftSlave().getId()
                        + " et a pour machine esclave droit l'esclave la machine esclave"
                        + this.slave[j].getRightSlave().getId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    
    /***
     * décompose un tableau de byte en nbrEsclave tableau de byte
     * Renvoit une liste contenant tout les tableaux obtenus
     * @param fileContent
     * @return 
     */
    private List<byte[]> getMultipleByteArray(byte[] fileContent) {
        System.out.println("Longueur du contenu du fichier : " + fileContent.length);
        List<byte[]> resultat = new ArrayList<>();
        // calcul du nombre de tableau à créer
        int tailleTableauDeByte = fileContent.length / this.slaveNb;
        // calcul du nombre d'octets(bytes) en trop
        int bytePasAPorte = fileContent.length % this.slaveNb;
        int compteur = 0;
        
        // création des tableaux
        for (int i = 0; i < this.slaveNb; i++) {
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
                esclave[j] = fileContent[compteur];
                compteur++;
            }
            resultat.add(esclave);
        }
        return resultat;
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
        if(!this.isBuild) {
            this.construireArbreBinaire();
            this.isBuild = true;
        }
        
        // création d'un thread pour éviter la sauvegarde bloquante
        Thread fil;
        fil = new Thread(new Runnable() {
            @Override
            @SuppressWarnings({"element-type-mismatch", "CallToPrintStackTrace"})
            public void run() {
                System.out.println("Fil en cours d'éxécution");
                //On découpe notre tableau en nbSlave tableau de taille égal
                List<byte[]> fichierDivise = getMultipleByteArray(fileContent);
                //On découpe la liste en deux pour chacun des slaves fils du master
                List<byte[]> pesclaveGauche = new ArrayList<>(fichierDivise.subList(0, fichierDivise.size() / 2));
                List<byte[]> pesclaveDroit = new ArrayList<>(fichierDivise.subList(fichierDivise.size() / 2, fichierDivise.size()));
                try {
                    //Sauvegarde des données dans les slaves du master
                    leftSlave.subSave(filename, pesclaveGauche);
                    rightSlave.subSave(filename, pesclaveDroit);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                /* Une fois le thread terminé, il se retire
                * lui même de la liste, afin de retirer le lock
                */
                filelocked.get(filename).remove(this);
            }
        });
        // Sauvegarde des threads dans la liste correspondant à la Clé juste avant le décelnchement du Thread
        if (!this.filelocked.containsKey(filename)) {
                this.filelocked.put(filename, new ArrayList<Thread>());
        }
        this.filelocked.get(filename).add(fil);
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
        File fichier = new File(this.dfsRootFolder + File.separator + filename);
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

    @SuppressWarnings("CallToPrintStackTrace")
    private void verifieSauvegardeTerminee(String nomFichier) {
        // on attend que la sauvegarde soit terminé si une sauvegarde est en cours sur le nom de fichier, 
        if (this.filelocked.containsKey(nomFichier)) {
            for (Thread recuperer : this.filelocked.get(nomFichier)) { 
                try {
                    // On attends la fin de ce thread (avant de continuer l'exécution de recuperer)
                    recuperer.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private byte[] concat(byte[] byte1, byte[] byte2) {
        int tailletabByte1 = byte1.length;
        int tailleTabByte2 = byte2.length;
        byte[] resultat = new byte[tailletabByte1 + tailleTabByte2];
        System.arraycopy(byte1, 0, resultat, 0, tailletabByte1);
        System.arraycopy(byte2, 0, resultat, tailletabByte1, tailleTabByte2);
        return resultat;
    }
    
    private byte[] reconstruireTableauDeByte(List<byte[]> leftList, List<byte[]> rightList) {
        byte[] resultat = new byte[0];
        //Pour chaque élément de chaque liste, on concatène le tableau récupéré avec le précédent
        for (int i = 0; i < leftList.size(); i++) {
            resultat = this.concat(resultat, leftList.get(i));
        }
        for (int i = 0; i < rightList.size(); i++) {
            resultat = this.concat(resultat, rightList.get(i));
        }
        return resultat;
    }
    
    @Override
    public byte[] retrieveBytes(String filename) throws RemoteException {
        // on construit l'arbre si c'est le premier appel d'un esclave
        if (!this.isBuild) {
            this.construireArbreBinaire();
            this.isBuild = true;
        }
        //On attend la fin des sauvegardes
        this.verifieSauvegardeTerminee(filename);
        //On récupere les bytes contenus dans les machines esclaves
        List<byte[]> bytesGauche;
        bytesGauche = this.leftSlave.subRetrieve(filename);
        //Si rien n'est récupéré, on renvoit null
        if(bytesGauche == null) {
            return null;
        }
        List<byte[]> bytesDroit;
        bytesDroit = this.rightSlave.subRetrieve(filename);
        //On retorune un tableau de byte construit sur les tableaux récupérés
        return this.reconstruireTableauDeByte(bytesGauche, bytesDroit);
    }

    @Override
    public long getFileSize(String fileName) throws RemoteException {
        if(!isBuilded) {
            buildBinaryTree();
        }
        //Même méthode d'attente que dans le retrieveBytes().
        checkTerminatedSave(filename);
        long leftSize = leftSlave.getFileSubsize(filename);
        if(leftSize == -1) {
            System.err.println("Impossible de retrouver la taille, le fichier n'existe pas");
            return -1;
        }
        //on calcul et renvoit la taille total
        long rightSize = rightSlave.getFileSubsize(filename);
        return leftSize + rightSize;
    }

    
}
