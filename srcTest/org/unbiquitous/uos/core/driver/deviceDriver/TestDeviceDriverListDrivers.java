package org.unbiquitous.uos.core.driver.deviceDriver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.unbiquitous.json.JSONException;
import org.unbiquitous.json.JSONObject;
import org.unbiquitous.uos.core.Logger;
import org.unbiquitous.uos.core.UOSApplicationContext;
import org.unbiquitous.uos.core.driver.DeviceDriver;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDriver;
import org.unbiquitous.uos.core.messageEngine.dataType.json.JSONDriver;
import org.unbiquitous.uos.core.test.model.DummyDriver;

import junit.framework.TestCase;

public class TestDeviceDriverListDrivers extends TestCase {
private static final Logger logger = Logger.getLogger(TestDeviceDriverListDrivers.class);
	
	private static UOSApplicationContext context;
	
	private static int testNumber = 0;
	
	private static final int timeToWaitBetweenTests = 2000;
	
	protected void setUp() throws Exception {
		Thread.sleep(timeToWaitBetweenTests/2);
		logger.debug("\n\n######################### TEST "+testNumber+++" #########################\n\n");
		context = new UOSApplicationContext();
		context.init("org/unbiquitous/uos/core/deviceManager/ubiquitos");
		Thread.sleep(timeToWaitBetweenTests/2);
	};
	
	protected void tearDown() throws Exception {
		context.tearDown();
		System.gc();
	}
	
	public void testSendListDrivers() 
		throws UnknownHostException, IOException, InterruptedException, JSONException {
		
		String notifyMessage = "{type:'SERVICE_CALL_REQUEST',driver:'uos.DeviceDriver',service:'listDrivers'}";
		
		logger.debug("Sending Message:");
		logger.debug(notifyMessage);
		
		String response = sendReceive(notifyMessage);
		
		logger.debug("Returned Message:");
		logger.debug("["+response+"]");
		
		JSONObject jsonResponse = new JSONObject(response);
		
		assertEquals("SERVICE_CALL_RESPONSE",jsonResponse.opt("type"));
		
		assertNotNull(jsonResponse.optJSONObject("responseData"));
		assertNotNull(jsonResponse.optJSONObject("responseData").opt("driverList"));
		
		Map<String,String> testListDrivers = new HashMap<String,String>();
		
		UpDriver upDeviceDriver = (new DeviceDriver()).getDriver();
		JSONDriver jsonDeviceDriver = new JSONDriver(upDeviceDriver);
		
		testListDrivers.put("uos.DeviceDriver1", jsonDeviceDriver.toString());
		testListDrivers.put("defaultDeviceDriver", jsonDeviceDriver.toString());
		testListDrivers.put("uos.DeviceDriver3", jsonDeviceDriver.toString());
		testListDrivers.put("testListId", jsonDeviceDriver.toString());
		
		UpDriver upDummyDriver = (new DummyDriver()).getDriver();
		JSONDriver jsonDummyDriver = new JSONDriver(upDummyDriver);
		
		testListDrivers.put("dummyDriverId", jsonDummyDriver.toString());
		testListDrivers.put("DummyDriver6", jsonDummyDriver.toString());
		
		JSONObject expectedDriverList = new JSONObject();
		
		expectedDriverList.put("driverList", new JSONObject(testListDrivers).toString());
		
		assertEquals(expectedDriverList.toString(), jsonResponse.optJSONObject("responseData").toString());
	}
	
	public void testSendListDriversByDriverNameValid1() 
	throws UnknownHostException, IOException, InterruptedException, JSONException {
		
		String notifyMessage = "{type:'SERVICE_CALL_REQUEST',driver:'uos.DeviceDriver',parameters:{driverName:'uos.DeviceDriver'},service:'listDrivers'}";
		
		logger.debug("Sending Message:");
		logger.debug(notifyMessage);
		
		String response = sendReceive(notifyMessage);
		
		logger.debug("Returned Message:");
		logger.debug("["+response+"]");
		
		JSONObject jsonResponse = new JSONObject(response);
		
		assertEquals("SERVICE_CALL_RESPONSE",jsonResponse.opt("type"));
		
		assertNotNull(jsonResponse.optJSONObject("responseData"));
		assertNotNull(jsonResponse.optJSONObject("responseData").opt("driverList"));
		
		Map<String,String> testListDrivers = new HashMap<String,String>();
		
		UpDriver upDriver = (new DeviceDriver()).getDriver();
		
		JSONDriver jsonDriver = new JSONDriver(upDriver);
		
		testListDrivers.put("uos.DeviceDriver1", jsonDriver.toString());
		testListDrivers.put("uos.DeviceDriver3", jsonDriver.toString());
		testListDrivers.put("defaultDeviceDriver", jsonDriver.toString());
		testListDrivers.put("testListId", jsonDriver.toString());
		
		JSONObject expectedDriverList = new JSONObject();
		
		expectedDriverList.put("driverList", new JSONObject(testListDrivers).toString());
		
		assertEquals(expectedDriverList.toMap(), jsonResponse.optJSONObject("responseData").toMap());
	}
	
	public void testSendListDriversByDriverNameValid2() 
	throws UnknownHostException, IOException, InterruptedException, JSONException {
		
		String notifyMessage = "{type:'SERVICE_CALL_REQUEST',driver:'uos.DeviceDriver',parameters:{driverName:'DummyDriver'},service:'listDrivers'}";
		
		logger.debug("Sending Message:");
		logger.debug(notifyMessage);
		
		String response = sendReceive(notifyMessage);
		
		logger.debug("Returned Message:");
		logger.debug("["+response+"]");
		
		JSONObject jsonResponse = new JSONObject(response);
		
		assertEquals("SERVICE_CALL_RESPONSE",jsonResponse.opt("type"));
		
		assertNotNull(jsonResponse.optJSONObject("responseData"));
		assertNotNull(jsonResponse.optJSONObject("responseData").opt("driverList"));
		
		Map<String,String> testListDrivers = new HashMap<String,String>();
		
		UpDriver upDummyDriver = (new DummyDriver()).getDriver();
		JSONDriver jsonDummyDriver = new JSONDriver(upDummyDriver);
		
		testListDrivers.put("dummyDriverId", jsonDummyDriver.toString());
		testListDrivers.put("DummyDriver6", jsonDummyDriver.toString());
		
		JSONObject expectedDriverList = new JSONObject();
		
		expectedDriverList.put("driverList", new JSONObject(testListDrivers).toString());
		
		assertEquals(expectedDriverList.toString(), jsonResponse.optJSONObject("responseData").toString());
	}
	
	public void testSendListDriversByDriverNameEmpty() 
	throws UnknownHostException, IOException, InterruptedException, JSONException {
		
		String notifyMessage = "{type:'SERVICE_CALL_REQUEST',driver:'uos.DeviceDriver',parameters:{driverName:''},service:'listDrivers'}";
		
		logger.debug("Sending Message:");
		logger.debug(notifyMessage);
		
		String response = sendReceive(notifyMessage);
		
		logger.debug("Returned Message:");
		logger.debug("["+response+"]");
		
		JSONObject jsonResponse = new JSONObject(response);
		
		assertEquals("SERVICE_CALL_RESPONSE",jsonResponse.opt("type"));
		
		assertNotNull(jsonResponse.optJSONObject("responseData"));
		assertNotNull(jsonResponse.optJSONObject("responseData").opt("driverList"));
		
		Map<String,String> testListDrivers = new HashMap<String,String>();
		
		JSONObject expectedDriverList = new JSONObject();
		
		expectedDriverList.put("driverList", new JSONObject(testListDrivers).toString());
		
		assertEquals(expectedDriverList.toString(), jsonResponse.optJSONObject("responseData").toString());
	}
	
	public void testSendListDriversByDriverNameWrong() 
	throws UnknownHostException, IOException, InterruptedException, JSONException {
		
		String notifyMessage = "{type:'SERVICE_CALL_REQUEST',driver:'uos.DeviceDriver',parameters:{driverName:'no.exists.driver.name'},service:'listDrivers'}";
		
		logger.debug("Sending Message:");
		logger.debug(notifyMessage);
		
		String response = sendReceive(notifyMessage);
		
		logger.debug("Returned Message:");
		logger.debug("["+response+"]");
		
		JSONObject jsonResponse = new JSONObject(response);
		
		assertEquals("SERVICE_CALL_RESPONSE",jsonResponse.opt("type"));
		
		assertNotNull(jsonResponse.optJSONObject("responseData"));
		assertNotNull(jsonResponse.optJSONObject("responseData").opt("driverList"));
		
		Map<String,String> testListDrivers = new HashMap<String,String>();
		
		JSONObject expectedDriverList = new JSONObject();
		
		expectedDriverList.put("driverList", new JSONObject(testListDrivers).toString());
		
		assertEquals(expectedDriverList.toString(), jsonResponse.optJSONObject("responseData").toString());
	}
	
	
	
	
	
	
	
	private static String sendReceive(String message) throws UnknownHostException, IOException, InterruptedException{
		Socket socket = new Socket("localhost",14984/*EthernetTCPConnectionManager.UBIQUITOS_ETH_TCP_PORT*/);
		
		OutputStream outputStream = socket.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		
		writer.write(message);
		writer.write('\n');
		writer.flush();
		Thread.sleep(1000);
		
		InputStream inputStream = socket.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		StringBuilder builder = new StringBuilder();
		if (reader.ready()){
        	for(Character c = (char)reader.read();c != '\n';c = (char)reader.read()){
        		builder.append(c);
        	}
		}
		socket.close();
		if (builder.length() == 0){
			return null;
		}
		return builder.toString(); 
	}
}
