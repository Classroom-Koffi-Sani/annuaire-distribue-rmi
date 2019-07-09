package fr.unice.miage.sd.tinydfs.nodes;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Slave extends Remote {

	public int getId() throws RemoteException;
	
	public Slave getLeftSlave() throws RemoteException;
	
	public Slave getRightSlave()  throws RemoteException;

	public void setLeftSlave(Slave slave) throws RemoteException;

	public void setRightSlave(Slave slave) throws RemoteException;
	
	public void subSave(String filename, List<byte[]> subFileContent) throws RemoteException;

	public List<byte[]> subRetrieve(String filename) throws RemoteException;

}
