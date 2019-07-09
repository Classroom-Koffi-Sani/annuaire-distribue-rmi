/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.unice.miage.sd.tinydfs.nodes;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Stekos
 */
public interface FileSizeInterface extends Remote {
    public long getFileSize(String fileName) throws RemoteException;
}
