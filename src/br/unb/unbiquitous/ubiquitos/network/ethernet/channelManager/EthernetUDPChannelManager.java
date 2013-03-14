package br.unb.unbiquitous.ubiquitos.network.ethernet.channelManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.unb.unbiquitous.ubiquitos.network.connectionManager.ChannelManager;
import br.unb.unbiquitous.ubiquitos.network.ethernet.EthernetDevice;
import br.unb.unbiquitous.ubiquitos.network.ethernet.connection.EthernetUDPClientConnection;
import br.unb.unbiquitous.ubiquitos.network.ethernet.connection.EthernetUDPServerConnection;
import br.unb.unbiquitous.ubiquitos.network.ethernet.connectionManager.EthernetConnectionManager.EthernetConnectionType;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.network.model.connection.ClientConnection;

public class EthernetUDPChannelManager implements ChannelManager{
	
	/*********************************
	 * ATTRIBUTES
	 *********************************/

	private List<NetworkDevice> freePassiveDevices;
	
	private Map<String, EthernetUDPServerConnection> startedServers;
	
	private int defaultPort;
	private int controlPort;
	
	
	/*********************************
	 * CONSTRUCTORS
	 *********************************/
	
	public EthernetUDPChannelManager(int defaultPort, int controlPort, String portRange){
		
		this.defaultPort = defaultPort;
		this.controlPort = controlPort;
		
		this.startedServers = new HashMap<String, EthernetUDPServerConnection>();
		
		freePassiveDevices = new ArrayList<NetworkDevice>();
		String[] limitPorts = portRange.split("-");
		int inferiorPort = Integer.parseInt(limitPorts[0]);
		int superiorPort = Integer.parseInt(limitPorts[1]);
		for(int port = inferiorPort; port <= superiorPort; port++){
			freePassiveDevices.add(new EthernetDevice("0.0.0.0",port,EthernetConnectionType.UDP));
		}
	}
	
	/********************************
	 * PUBLIC METHODS
	 ********************************/
	
	public ClientConnection openActiveConnection(String networkDeviceName) throws NetworkException, IOException{
		String[] address = networkDeviceName.split(":");
		
		String host ;
		int port ;
		if (address.length == 1){
			port = controlPort;
		}else if(address.length == 2){
			port = Integer.parseInt(address[1]);
		}else{
			throw new NetworkException("Invalid parameters for creation of the channel.");
		}
		
    	host = address[0];
    	
    	EthernetUDPClientConnection etcc ;
		try {
			etcc = new EthernetUDPClientConnection(host, port);
		} catch (Exception e) {
			etcc = new EthernetUDPClientConnection(host, defaultPort);
		}
		return etcc;
	}

	public ClientConnection openPassiveConnection(String networkDeviceName) throws NetworkException, IOException{
		String[] address = networkDeviceName.split(":");
		
		if(address.length != 2){
			throw new NetworkException("Invalid parameters for creation of the channel.");
		}
		
		EthernetUDPServerConnection server = startedServers.get(networkDeviceName);
		if(server == null){
			String host = address[0];
	    	int port = Integer.parseInt(address[1]);
			
	    	server = new EthernetUDPServerConnection(new EthernetDevice(host, port, EthernetConnectionType.UDP));
	    	startedServers.put(networkDeviceName, server);
		}
		
		return server.accept();
	}
	
	
	public NetworkDevice getAvailableNetworkDevice(){
		NetworkDevice networkDevice = freePassiveDevices.remove(0);
		freePassiveDevices.add(networkDevice);
		return networkDevice;
	}
	
	
	public void tearDown() throws NetworkException, IOException {
		for(EthernetUDPServerConnection server : startedServers.values()){
			server.closeConnection();
		}
	}

}
