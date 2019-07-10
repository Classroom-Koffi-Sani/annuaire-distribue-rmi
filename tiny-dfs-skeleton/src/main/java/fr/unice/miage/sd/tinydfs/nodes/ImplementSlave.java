/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.unice.miage.sd.tinydfs.nodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Stekos
 */
public class ImplementSlave implements Slave{
    
    private int idSlave;
    private String dfsRootFolder;
    private Slave leftSlave, rightSlave;
    
    public ImplementSlave(int idSlave, String dfsRootFolder) throws RemoteException {
        super();
        this.idSlave = idSlave;
        this.dfsRootFolder = dfsRootFolder;
        File dfsFileRootFolder = new File(dfsRootFolder);
        if (!dfsFileRootFolder.exists()) {
                dfsFileRootFolder.mkdir();
                System.out.println("Création du dossier " + dfsFileRootFolder.getName());
        }
    }

    @Override
    public int getId() throws RemoteException {
        return this.idSlave;
    }

    @Override
    public Slave getLeftSlave() throws RemoteException {
        return this.leftSlave;
    }

    @Override
    public Slave getRightSlave() throws RemoteException {
        return this.rightSlave;
    }

    @Override
    public void setLeftSlave(Slave slave) throws RemoteException {
        this.leftSlave = slave;
    }

    @Override
    public void setRightSlave(Slave slave) throws RemoteException {
        this.rightSlave = slave;
    }
    
    private void subSaveToDisk(String filename, byte[] fileContent) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(dfsRootFolder + File.separator + filename)) {
            stream.write(fileContent);
        }
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void subSave(String filename, List<byte[]> subFileContent) throws RemoteException {
        int sizeList, middleList;
        middleList = (sizeList = subFileContent.size()) / 2;
        try {// Sauvegarde du fragment au milieu de la liste
                subSaveToDisk(idSlave + filename, subFileContent.get(middleList));
        } catch (IOException e) {
                System.err.println("Erreur d'écriture du fichier " + idSlave + filename);
                e.printStackTrace();
        }
        if (middleList != 0) { // Envoi des parties gauches et droit de la liste
                                                        // aux laves fils
                List<byte[]> left = new ArrayList<>(subFileContent.subList(0, middleList));
                List<byte[]> right = new ArrayList<>(subFileContent.subList(middleList + 1, sizeList));
                leftSlave.subSave(filename, left);
                rightSlave.subSave(filename, right);
        }
    }
    
    @SuppressWarnings("CallToPrintStackTrace")
    private byte[] subRetrieveFromDisk(String filename) {
        Path path = Paths.get(dfsRootFolder + File.separator + filename);
        byte[] data = null;
        try {
                data = Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Une erreur c'est produite lors de la lecture du fichier " + dfsRootFolder + File.separator + filename);
            e.printStackTrace();
        }
        return data;
    }

    @Override
    public List<byte[]> subRetrieve(String filename) throws RemoteException {
        if (!(new File(dfsRootFolder + File.separator + idSlave + filename)).exists()) {
            System.err.println("Oups !!! Le fichier " + idSlave + filename + " nexiste pas ... !");
            return null; // Retourne null si le ficher n'existe pas
        }

        if (leftSlave == null) { // retourne une liste de un fragment si le
                                                            // slave n'a pas de fils
            return new ArrayList<>(Arrays.asList(subRetrieveFromDisk(idSlave + filename)));
        }
        List<byte[]> responsableList = leftSlave.subRetrieve(filename);
        responsableList.add(subRetrieveFromDisk(idSlave + filename));
        responsableList.addAll(rightSlave.subRetrieve(filename));
        return responsableList; // retourne la liste unie des fragments
    }

    @Override
    public long getFileSubsize(String filename) throws RemoteException {
        File f = new File(dfsRootFolder + File.separator + idSlave + filename);
        // on renvoit -1, si le fichier n'existe pas, 
        if(!f.exists() || f.isDirectory()) {
                System.out.println("Esclave" + idSlave + "ta mère");
                return -1;
        }
        //Si je suis une feuille, je renvoie juste la taille du fichier que je stocke
        if(rightSlave == null) {
                return f.length();
        }
        //caclule de la taille totale
        long rightSize = rightSlave.getFileSubsize(filename);
        long leftSize = leftSlave.getFileSubsize(filename);
        long result = leftSize + f.length() + rightSize;
        return result;
    }
    
    
    class filterSlave implements FilenameFilter {

        @Override
        public boolean accept(File folder, String name) {
                return Pattern.compile("^" + idSlave).matcher(name).matches();
        }

    }
}
