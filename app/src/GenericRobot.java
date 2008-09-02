// jade
import jade.core.*;

public class GenericRobot extends Agent {
	// Constants
	/////////////////////////////////////////////////////////////////////////////
	private final String 	STAGE_IP 			= "172.16.95.128";

	// Static variables
	/////////////////////////////////////////////////////////////////////////////

	// Instance variables
	/////////////////////////////////////////////////////////////////////////////

	// Constructors
	/////////////////////////////////////////////////////////////////////////////

	// External methods
	/////////////////////////////////////////////////////////////////////////////

	protected void setup() throws PlayerException {
		System.out.println("GenericRobot starting up as " + getAID().getName());

		player 	= null;
		
		// check command line parameters
		Object[] args = getArguments();
		if (args == null || args.length != 5) {
			System.out.println("# GenericRobot:");			
			System.out.println("Please supply parameters port, from x&y, to x&y");
			doDelete();
			return;
		}
		
		// start proxies and interfaces
		try {
			int port 	= Integer.parseInt((String) args[0]);
		} catch (Exception e) {
			System.out.println("Unknown exception thrown:\n" + e.message);
			
		}
		
		// do something
		float distance = estimateDistance();
		System.out.println("Distance estimate is " + distance);
	}
	
	// pre-death clean up
	protected void takeDown() {
		System.out.println("Goodbye from " + getAID().getName());
	}

	// Internal methods
	/////////////////////////////////////////////////////////////////////////////

	// uses planner interface to estimate distance to target
	public float estimateDistance() {
	}
	
	//===========================================================================
	// Inner class definitions
	//===========================================================================
}