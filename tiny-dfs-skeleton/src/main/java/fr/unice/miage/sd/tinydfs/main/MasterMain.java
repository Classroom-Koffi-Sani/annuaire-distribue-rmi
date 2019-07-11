package fr.unice.miage.sd.tinydfs.main;

import fr.unice.miage.sd.tinydfs.nodes.ImplementMaster;
import fr.unice.miage.sd.tinydfs.nodes.Master;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class MasterMain {

    // Usage: java fr.unice.miage.sd.tinydfs.main.MasterMain storage_service_name dfs_root_folder nb_slaves
    public static void main(String[] args) throws RemoteException {
        String storageServiceName = args[0];
        String dfsRootFolder = args[1];
        int nbSlaves = Integer.parseInt(args[2]);

        // Create master and register it
        Registry registry = LocateRegistry.createRegistry(1099);
        Master objMaster;
        try {
            //Initialise le master
            objMaster = new ImplementMaster(dfsRootFolder, nbSlaves);
            //Enregistre le master dans le RMI
            registry.bind(storageServiceName, objMaster);
            System.out.println("Master prêt et disponible à l'adresse:" +
                    "rmi://" + InetAddress.getLocalHost().getHostAddress() + ":1099/" + storageServiceName);
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
	
}
