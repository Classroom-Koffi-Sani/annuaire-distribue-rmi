package fr.unice.miage.sd.tinydfs.main;


public class SlaveMain { 
	
	// Usage: java fr.unice.miage.sd.tinydfs.main.SlaveMain master_host dfs_root_folder slave_identifier
	public static void main(String[] args) {
		String masterHost = args[0];
		String dfsRootFolder = args[1];
		int slaveId = Integer.parseInt(args[2]);
		
		// Create slave and register it (registration name must be "slave" + slave identifier)
		
	}

}