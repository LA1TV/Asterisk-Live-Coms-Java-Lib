package tjenkinson.asteriskLiveComsLib;

import java.io.IOException;

import douglascrockford.json.JSONException;
import tjenkinson.asteriskLiveComsLib.program.LiveComsServer;
import tjenkinson.asteriskLiveComsLib.program.events.AsteriskLiveComsEvent;
import tjenkinson.asteriskLiveComsLib.program.events.AsteriskLiveComsEventListener;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelAddedEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelRemovedEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelToHoldingEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelVerifiedEvent;
import tjenkinson.asteriskLiveComsLib.program.events.ChannelsToRoomEvent;
import tjenkinson.asteriskLiveComsLib.program.exceptions.AccessAlreadyGrantedException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.AlreadyConnectedException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.ChannelIDInvalidException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.NotConnectedException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.RequestJSONInvalidException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.UnableToConnectException;
import tjenkinson.asteriskLiveComsLib.program.exceptions.UnknownException;

public class TestProgram implements AsteriskLiveComsEventListener {

	/**
	 * @param args
	 * @throws AlreadyConnectedException 
	 * @throws UnableToConnectException 
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws NotConnectedException 
	 * @throws UnknownException 
	 */
	public static void main(String[] args) throws UnableToConnectException, AlreadyConnectedException, JSONException, IOException, NotConnectedException, UnknownException {
		
		new TestProgram();
	}
	LiveComsServer a;

	public TestProgram() throws UnableToConnectException, AlreadyConnectedException, JSONException, IOException, NotConnectedException, UnknownException {
		a = new LiveComsServer("127.0.0.1", 2345);
		a.addEventListener(this);
		a.connect();
		System.out.println(a.getChannels());
	}
	
	@Override
	public void onAsteriskLiveComsEvent(AsteriskLiveComsEvent e) {
		System.out.println("Received event: "+e.toString());
		if (e.getClass().getSimpleName().equals("ChannelAddedEvent")) {
			System.out.println(((ChannelAddedEvent) e).getInfo());
			System.out.println("Has id: "+((ChannelAddedEvent) e).getInfo().get("id"));
			try {
				a.grantAccess((int) ((ChannelAddedEvent) e).getInfo().get("id"), true);
			} catch (JSONException | NotConnectedException
					| RequestJSONInvalidException | ChannelIDInvalidException
					| UnknownException | AccessAlreadyGrantedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if (e.getClass().getSimpleName().equals("ChannelRemovedEvent")) {
			System.out.println(((ChannelRemovedEvent) e).getChannelId());
		}
		else if (e.getClass().getSimpleName().equals("ChannelsToRoomEvent")) {
			System.out.println(((ChannelsToRoomEvent) e).getChannelIds());
		}
		else if (e.getClass().getSimpleName().equals("ChannelToHoldingEvent")) {
			System.out.println(((ChannelToHoldingEvent) e).getChannelId());
		}
		else if (e.getClass().getSimpleName().equals("ChannelVerifiedEvent")) {
			System.out.println(((ChannelVerifiedEvent) e).getChannelId());
		}
		else if (e.getClass().getSimpleName().equals("ServerResettingEvent")) {
			System.out.println("Server resetting.");
		}
		else if (e.getClass().getSimpleName().equals("ConnectionLostEvent")) {
			System.out.println("Connection lost.");
		}
	}

}
