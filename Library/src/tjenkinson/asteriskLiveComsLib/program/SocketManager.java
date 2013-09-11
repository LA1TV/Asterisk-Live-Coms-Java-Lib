package tjenkinson.asteriskLiveComsLib.program;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tjenkinson.asteriskLiveComsLib.program.events.AsteriskLiveComsEvent;
import tjenkinson.asteriskLiveComsLib.program.events.AsteriskLiveComsEventListener;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelAddedEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelRemovedEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelToHoldingEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelVerifiedEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelsToRoomEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ConnectionLostEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ServerResettingEvent;
import tjenkinson.asteriskLiveComsLib.program.exceptions.AlreadyConnectedException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.NotConnectedException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.UnableToConnectException;
import douglascrockford.json.JSONArray;
import douglascrockford.json.JSONException;
import douglascrockford.json.JSONObject;

public class SocketManager {

    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    
    private PipedOutputStream incomingEventOutStream = null;
    private PrintWriter incomingEventOut = null;
    private BufferedReader incomingEventIn = null;
    private PipedOutputStream incomingResponsOutStream = null;
    private PrintWriter incomingResponseOut = null;
    private BufferedReader incomingResponseIn = null;
    
    private ArrayList<AsteriskLiveComsEventListener> listeners = new ArrayList<AsteriskLiveComsEventListener>();
    private final ExecutorService eventsDispatcherExecutor;
    
    private String ip;
    private int port;

    private Object socketLock = new Object();

    public SocketManager(String ip, int port) {
        this.ip = ip;
        this.port = port;
        eventsDispatcherExecutor = Executors.newSingleThreadExecutor();
    }

    public void connect() throws UnableToConnectException, AlreadyConnectedException {
        synchronized(socketLock) {
            if (socket != null && !socket.isClosed()) {
                throw (new AlreadyConnectedException());
            }
            try {
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                incomingEventOutStream = new PipedOutputStream();
                incomingEventIn = new BufferedReader(new InputStreamReader(new PipedInputStream(incomingEventOutStream)));
                incomingEventOut = new PrintWriter(incomingEventOutStream);
                
                incomingResponsOutStream = new PipedOutputStream();
                incomingResponseIn = new BufferedReader(new InputStreamReader(new PipedInputStream(incomingResponsOutStream)));
                incomingResponseOut = new PrintWriter(incomingResponsOutStream);
                
            } catch (IOException e) {
                throw (new UnableToConnectException());
            }
            new Thread(new IncomingEventThread()).start();
            new Thread(new SocketThread()).start();
        }
    }

    public void disconnect() throws NotConnectedException {
    	disconnect(false);
    }
    	
    private void disconnect(boolean notRequested) throws NotConnectedException {
        synchronized(socketLock) {
            if (!isConnected()) {
                throw (new NotConnectedException());
            }
            incomingEventOut.close();
            try {
				incomingEventIn.close();
			} catch (IOException e2) {}
            try {
				incomingResponseIn.close();
			} catch (IOException e1) {}
            incomingResponseOut.close();
            try {
                socket.shutdownInput();
            } catch (IOException e) {}
            try {
                socket.shutdownOutput();
            } catch (IOException e) {}
            try {
                socket.close();
            } catch (IOException e) {}
            if (notRequested) {
            	dispatchEvent(new ConnectionLostEvent());
            }
        }
    }

    public boolean isConnected() {
        synchronized(socketLock) {
            return (socket != null && !socket.isClosed());
        }
    }
    
    public void addEventListener(AsteriskLiveComsEventListener a) {
    	synchronized(listeners) {
    		listeners.add(a);
    	}
    }
    
    public void removeEventListener(AsteriskLiveComsEventListener a) {
    	synchronized(listeners) {
    		listeners.remove(a);
    	}
    }
    
    private void dispatchEvent(final AsteriskLiveComsEvent e) {
    	synchronized (listeners) {
			synchronized (eventsDispatcherExecutor) {
				eventsDispatcherExecutor.execute(new Runnable()
		        {
		            public void run()
		            {
		            	for(int i=0; i<listeners.size(); i++) {
		            		listeners.get(i).onAsteriskLiveComsEvent(e);
		            	}
		            }
		        });
			}
    	}
	}
    
    public JSONObject sendRequest(JSONObject request) throws JSONException, NotConnectedException {
    	synchronized(socketLock) {
	    	System.out.println("sending request "+request.toString());
	    	out.println(request.toString());
	    	try {
				return new JSONObject(incomingResponseIn.readLine());
			} catch (IOException e) {
				// lets close the connection
				try {
					disconnect(true);
				} catch (NotConnectedException e1) {}
				throw(new NotConnectedException());
			}
    	}
    }

    private class SocketThread implements Runnable {

        @Override
        public void run() {
            String inputLine = null;
            try {
                while((inputLine = in.readLine()) != null) {
                    // determine if this is a response or event and send to necessary location
                	JSONObject lineJSON = new JSONObject(inputLine);
                	if (lineJSON.getString("type").equals("response")) {
                		incomingResponseOut.println(inputLine);
                		incomingResponseOut.flush();
                	}
                	else if (lineJSON.getString("type").equals("event")) {
                		incomingEventOut.println(inputLine);
                		incomingEventOut.flush();
                	}
                }

                if (isConnected()) {
                    try {
                        disconnect(true);
                    } catch (NotConnectedException e) {}
                }
            } catch (IOException e) {
                // try and disconnect (if not already disconnected) and end thread
                if (isConnected()) {
                    try {
                        disconnect(true);
                    } catch (NotConnectedException e1) {}
                }
            }
        }

    }
    
    private class IncomingEventThread implements Runnable {

        @Override
        public void run() {
            String inputLine = null;
            try {
                while((inputLine = incomingEventIn.readLine()) != null) {
                	synchronized(socketLock) {
	                	JSONObject lineJSON = new JSONObject(inputLine);
	                	String eventType = lineJSON.getString("eventType");
	                	// determine what type of event it is and then fire one that represents it
	                	if (eventType.equals("channelAdded")) {
	                		JSONObject a = lineJSON.getJSONObject("payload");
	                		Hashtable<String,Object> data = new Hashtable<String,Object>();
	            			Object[] keys = a.keySet().toArray();
	            			for(int i=0; i<keys.length; i++) {
	            				data.put((String) keys[i], a.get((String) keys[i]));
	            			}
	                		dispatchEvent(new ChannelAddedEvent(data));
	                	}
	                	else if (eventType.equals("channelRemoved")) {
	                		dispatchEvent(new ChannelRemovedEvent(lineJSON.getJSONObject("payload").getInt("channelId")));
	                	}
	                	else if (eventType.equals("channelsToRoom")) {
	                		ArrayList<Integer> data = new ArrayList<Integer>();
	                		JSONObject a = lineJSON.getJSONObject("payload");
	                		JSONArray ids = a.getJSONArray("channelIds");
	                		for(int i=0; i<ids.length(); i++) {
	                			data.add(ids.getInt(i));
	                		}
	                		dispatchEvent(new ChannelsToRoomEvent(data));
	                	}
	                	else if (eventType.equals("channelToHolding")) {
	                		dispatchEvent(new ChannelToHoldingEvent(lineJSON.getJSONObject("payload").getInt("channelId")));
	                	}
	                	else if (eventType.equals("channelVerified")) {
	                		dispatchEvent(new ChannelVerifiedEvent(lineJSON.getJSONObject("payload").getInt("channelId")));
	                	}
	                	else if (eventType.equals("serverResetting")) {
	                		dispatchEvent(new ServerResettingEvent());
	                	}
                	}
                }
            } catch (IOException e) {}
        }

    }
}