package fr.unice.miage.sd.tinydfs.main;

import fr.unice.miage.sd.tinydfs.nodes.ImplementSlave;
import fr.unice.miage.sd.tinydfs.nodes.Slave;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;


public class SlaveMain { 
	
    // Usage: java fr.unice.miage.sd.tinydfs.main.SlaveMain master_host dfs_root_folder slave_identifier
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) throws RemoteException, Exception {
        String masterHost = args[0];
        String dfsRootFolder = args[1];
        int slaveId = Integer.parseInt(args[2]);

        // Create slave and register it (registration name must be "slave" + slave identifier)
        Slave objSlave= new ImplementSlave(slaveId, dfsRootFolder);
        try {
            String url = "rmi://" + masterHost + "/esclave" + slaveId;

            boolean result = checkRegisterSlave(url, slaveId);
            if(!result)
            {
                Naming.bind(url, objSlave);
                System.out.println("slave "+slaveId + " enregistré dans le RMI");
            }

        } catch (MalformedURLException e) {
            System.err.println("Erreur :\n");
            e.printStackTrace();
        } catch(AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkRegisterSlave(String url, int slaveId) throws Exception {
        boolean result =true;
        try {
            Remote r = Naming.lookup(url);
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            result=false;
        }
        
        if(result)
        {
            throw new Exception("Esclave" + slaveId + " est déja enregistré.");
        }
        
        return result;
    }

}