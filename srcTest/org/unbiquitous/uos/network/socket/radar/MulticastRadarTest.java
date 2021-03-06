package org.unbiquitous.uos.network.socket.radar;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.unbiquitous.uos.core.InitialProperties;
import org.unbiquitous.uos.core.network.connectionManager.ConnectionManager;
import org.unbiquitous.uos.core.network.model.NetworkDevice;
import org.unbiquitous.uos.core.network.radar.Radar;
import org.unbiquitous.uos.core.network.radar.RadarListener;
import org.unbiquitous.uos.network.socket.SocketDevice;

public class MulticastRadarTest extends TestCase {

	private static final String THREAD_NAME = "radar-t";
	private Integer port;
	private MulticastRadar radar;
	private MulticastSocket serverSocket;
	private DatagramSocketFactory factory;
	private RadarListener listener;

	public void setUp() throws Exception{
		port = 15000;
		//TODO: port is not used
		ResourceBundle bundle = new ListResourceBundle() {
			protected Object[][] getContents() {
				return new Object[][] {
						{ "ubiquitos.eth.tcp.port", port.toString() },
						{ "ubiquitos.multicast.beaconFrequencyInSeconds", 30 }
				}; 
			}
		};
		
		listener = mock(RadarListener.class);
		radar = new MulticastRadar(listener);
		ConnectionManager mng = mock(ConnectionManager.class);
		when(mng.getProperties()).thenReturn(new InitialProperties(bundle));
		radar.setConnectionManager(mng);
		mockSockets();
	}

	private void mockSockets() throws IOException {
		serverSocket = mock(MulticastSocket.class);
		factory = mock(DatagramSocketFactory.class);
		when(factory.newSocket(port)).thenReturn(serverSocket);
		radar.socketFactory = factory;
	}
	
	public void tearDown(){
		radar.stopRadar();
		DateTimeUtils.setCurrentMillisSystem();
	}
	
	public void test_mustBeARadarWithAProperFactory(){
		radar = new MulticastRadar(null);
		assertThat(radar).isInstanceOf(Radar.class);
		assertThat(radar.socketFactory)
			.isNotNull()
			.isInstanceOf(DatagramSocketFactory.class);
	}
	
	public void test_listenToThePortSpecifyedWithA10sTimeout() throws Throwable{
		doIt();
		assertEventually(1000, new Runnable() {
			public void run() {
				try {
					verify(factory).newSocket(port);
					verify(serverSocket,times(1)).setSoTimeout(10*1000);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	
	public void test_waitForDeviceToNotifyItsExistence() throws Exception{
		doIt();
		
		ArgumentCaptor<DatagramPacket> arg = forClass(DatagramPacket.class); 
		verify(serverSocket,atLeastOnce()).receive(arg.capture());
		assertThat(arg.getValue()).isNotNull();
	}
	
	public void test_doesNotFailOnATimeout() throws Exception{
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation)throws Throwable {
				throw new SocketTimeoutException("Test exception");
			}
		}).when(serverSocket).receive((DatagramPacket)any());
		doIt();
	}
	
	public void test_startAndStopMustControlTheNumberOfThreads() throws Throwable{
		doIt();
		assertEventually(1000, new Runnable() {
			public void run() {
				Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
				for (Thread t : allStackTraces.keySet()){
					if(t.getName().equals(THREAD_NAME)){
						return;
					}
				}
				throw new AssertionError("Thread "+THREAD_NAME+" not found");
			}
		});
		
		radar.stopRadar();
		assertEventually(1000, new Runnable() {
			public void run() {
				Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
				for (Thread t : allStackTraces.keySet()){
					if(t.getName().equals(THREAD_NAME)){
						throw new AssertionError("Thread "+THREAD_NAME+" not expected");
					}
				}
			}
		});
		assertEventually(1000, new Runnable() {
			public void run() {
					verify(serverSocket,times(1)).close();
			}
		});
	}
	
	public void test_sendsABroadcastBeaconAtStartup() throws Throwable{
		doIt();
		final ArgumentCaptor<DatagramPacket> arg = forClass(DatagramPacket.class);
		assertEventually(1000, new Runnable() {
			public void run() {
				try {
					verify(serverSocket,times(1)).send(arg.capture());
					DatagramPacket beacon = arg.getValue();
					assertThat(beacon.getAddress().getHostAddress())
						.isEqualTo("255.255.255.255");
					assertThat(beacon.getPort())
						.isEqualTo(port);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	
	public void test_whenSomebodySendsABeaconNotifiesItsDiscoveryOnce() throws Throwable{
		mockADeviceEntry("1.1.1.1","2.2.2.2","3.3.3.3");
		doIt();
		final ArgumentCaptor<NetworkDevice> arg = forClass(NetworkDevice.class);
		assertEventually(1000, new Runnable() {
			public void run() {
				verify(listener,times(3)).deviceEntered(arg.capture());
				NetworkDevice device = arg.getValue();
				assertThat(device).isInstanceOf(SocketDevice.class);
				NetworkDevice device1 = arg.getAllValues().get(0);
				assertThat(device1.getNetworkDeviceName())
												.isEqualTo("1.1.1.1"+":"+port);
				NetworkDevice device2 = arg.getAllValues().get(1);
				assertThat(device2.getNetworkDeviceName())
												.isEqualTo("2.2.2.2"+":"+port);
				NetworkDevice device3 = arg.getAllValues().get(2);
				assertThat(device3.getNetworkDeviceName())
												.isEqualTo("3.3.3.3"+":"+port);
			}
		});
	}

	public void test_sendsADirectResponseBeaconWhenSomebodyIsFound() throws Throwable{
		mockADeviceEntry("1.1.1.1");
		doIt();
		final ArgumentCaptor<DatagramPacket> arg = forClass(DatagramPacket.class);
		assertEventually(1000, new Runnable() {
			public void run() {
				try {
					verify(serverSocket,times(2)).send(arg.capture());
					DatagramPacket beacon = arg.getAllValues().get(1);
					assertThat(beacon.getAddress().getHostAddress())
						.isEqualTo("1.1.1.1");
					assertThat(beacon.getPort())
						.isEqualTo(port);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	
	//TODO: How to detect device left ?
	
	@SuppressWarnings("serial")
	public void test_checkForLeftDevicesEvery30seconds() throws Throwable{
		final List<String> enteredAddress = new ArrayList<String>(){
			{
				add("1.1.1.1");add("2.2.2.2");add("3.3.3.3");
			}
		};
		 
		doAnswer(new Answer<Void>() {
			int index = 0;
			public Void answer(InvocationOnMock invocation)
					throws Throwable {
				String addr = enteredAddress.get(index++ % enteredAddress.size() );
				Object[] args = invocation.getArguments();
				DatagramPacket packet = (DatagramPacket) args[0];
				packet.setAddress(InetAddress.getByName(addr));
				return null;
			}
		}).when(serverSocket).receive((DatagramPacket)any());
		
		doIt();
		Thread.sleep(10);
		enteredAddress.remove("2.2.2.2");
		
		final ArgumentCaptor<NetworkDevice> arg = forClass(NetworkDevice.class);
		assertEventually(1000, new Runnable() {
			public void run() {
				DateTimeUtils.setCurrentMillisFixed(new DateTime().plusSeconds(30).getMillis());
				verify(listener,times(1)).deviceLeft(arg.capture());
				NetworkDevice device = arg.getValue();
				assertThat(device.getNetworkDeviceName()).isEqualTo("2.2.2.2:"+port);
			}
		});
	}
	
	public void test_sendsABeaconEvery30seconds() throws Throwable{
		doIt();
		final ArgumentCaptor<DatagramPacket> arg = forClass(DatagramPacket.class);
		assertEventually(1000, new Runnable() {
			public void run() {
				try {
					DateTimeUtils.setCurrentMillisFixed(new DateTime().plusSeconds(30).getMillis());
					verify(serverSocket,times(2)).send(arg.capture());
					DatagramPacket beacon = arg.getAllValues().get(1);
					assertThat(beacon.getAddress().getHostAddress())
						.isEqualTo("255.255.255.255");
					assertThat(beacon.getPort())
						.isEqualTo(port);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	
	private void mockADeviceEntry(final String ... enteredAddress) throws IOException {
		doAnswer(new Answer<Void>() {
			int index = 0;
			public Void answer(InvocationOnMock invocation)
					throws Throwable {
				String addr = enteredAddress[index++ % enteredAddress.length ];
				Object[] args = invocation.getArguments();
				DatagramPacket packet = (DatagramPacket) args[0];
				packet.setAddress(InetAddress.getByName(addr));
				return null;
			}
		}).when(serverSocket).receive((DatagramPacket)any());
	}
	
	private void doIt(){
		radar.startRadar(); //TODO: why ?
		Thread t = new Thread(radar,THREAD_NAME);
		t.start();
	}
	
	private void assertEventually(int timeoutInMilliseconds, Runnable assertion) throws Throwable{
		long begin = System.currentTimeMillis();
		long now = begin;
		Throwable lastException = null;
		do{
			try{
				assertion.run();
				return;
			}catch(RuntimeException e){
				lastException = e;
			}catch(AssertionError e){
				lastException = e;
			}
			now = System.currentTimeMillis(); 
		}while((now - begin) < timeoutInMilliseconds);
		throw lastException;
	}
}
