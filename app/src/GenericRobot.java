/***
 * @author Adrian Schoenig, UNSW CSE, adrian.schoenig@gmail.com
 * @since 2008/07
 *
 * This JADE agent is modelling a robot which is implemented using the component-based
 * Orca framework. It requires a GoalEvaluator and a GoalPlanner component.
 *
 * Behaviours of this agent
 * - TickedBehaviour which checks the DF every minute to update it's list of nearby robots
 * - TODO: TickedBehaviour which keeps track of own tasks and actions
 * - CyclicBehaviour which listens for requests for adding new tasks
 * - TODO: ContractNetResponder which listens for CFP of ---TODO type--- to try to buy new goals
 * - TODO: ContractNetInitiator triggered by TickedBehaviour to occasionally sell it's own tasks
 *   executing new actions in order to achieve my goals/tasks and check whether I have
 *   recently achieved one of my goals/tasks
 */

import java.util.*;
import java.util.regex.*;

import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;

import talker.*;

public class GenericRobot extends Agent {
	// Constants
	/////////////////////////////////////////////////////////////////////////////
	enum DEBUG { SPAM, INFO, WARNING, ERROR };
	// TODO: this should be set some different way
	private final String[] DEFAULT_ARGS = {"--Ice.Default.Locator=IceGrid/Locator:default -p 12000"};
	private final String   GOAL_COORDS	= "[-23 8] [-23 1] [-21 -4] [-17 7] [-17 -2] [-17 -5] [-14 7] [-12 -5] [-10 7] [-9 0] [-7 7] [-6 -5] [-5 -1] [-2 8.5] [-1 3] [-1 0] [2 0] [2 -8] [5.5 7] [8 -8] [10 7] [11 0] [11 -8] [14 7] [14 0] [14 -5] [17 7] [20 7] [23 -4]";

	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	private ArrayList<AID> 		buyer_robots		= new ArrayList<AID>();
	private ArrayList<Task2d>	received_tasks	= new ArrayList<Task2d>();
	private Bundle2d					bundle					= new Bundle2d();
	private DEBUG 						debug_level 		= DEBUG.INFO;

	// Orca interfaces
	private Ice.Communicator 	ic;
	private PathEvaluatorPrx 	patheval; // used to find best bundles
	// TODO: add => goalplanner implements path follower: send current goal(s)
	// to make robot drive

	// Agent methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Called on start by JADE
	 */
	protected void setup() {
		log(DEBUG.INFO, "GenericRobot starting up.");
		
		// check command line parameters
		// 1: robot colour
		// 2: debug level
		Object[] args = getArguments();
		if (args == null || args.length != 2) {
			log(DEBUG.ERROR, "Necessary parameters missing.");
			log(DEBUG.ERROR, "Please supply parameters: <robot_colour> <debug_level>");
			log(DEBUG.ERROR, "For example: red info");
			doDelete();
			return;
		}
		String robo_colour = (String) args[0];
		this.debug_level = DEBUG.valueOf(((String) args[args.length - 1]).toUpperCase());
		
		try {
 			/**************************** ORCA PROXIES *****************************/
			log(DEBUG.INFO, "Connecting to Ice.");
			initProxies(robo_colour);
			
 			/************************** ADDING BEHAVIOURS **************************/
			log(DEBUG.INFO, "Adding behaviours.");
			
			// TickedBehaviour which keeps track of own tasks and actions
			addBehaviour(new RobotBehaviour(this, 10000));

			// TickedBehaviour which checks the DF every minute to update it's list of nearby agents
			addBehaviour(new UpdateNeighbourBehaviour(this, 60000));
			
			// TODO: CyclicBehaviour which listens for requests of for adding new goals
			addBehaviour(new TaskReceiverBehaviour(this));
			
			// TODO: CyclicBehaviour which listens for CFP to try to buy new goals
			// TODO: TickedBehaviour which occasionally tries to sell it's own tasks
			
 			/************************** PROVIDED SERVICES **************************/
			log(DEBUG.INFO, "Registering with DF.");

			// register my own capabilities with DF
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());

			// I can get assigned new tasks
			ServiceDescription sd_new_tasks = new ServiceDescription();
			sd_new_tasks.setType("task-collecting");
			sd_new_tasks.setName(getLocalName() + "-task-collecting");
			dfd.addServices(sd_new_tasks);
			
			// I can buy new tasks
			ServiceDescription sd_buy_tasks = new ServiceDescription();
			sd_buy_tasks.setType("task-buying");
			sd_buy_tasks.setName(getLocalName() + "-task-buying");
			dfd.addServices(sd_buy_tasks);

			// I can sell my tasks
			ServiceDescription sd_sell_tasks = new ServiceDescription();
			sd_sell_tasks.setType("task-selling");
			sd_sell_tasks.setName(getLocalName() + "-task-selling");
			dfd.addServices(sd_sell_tasks);
			
			DFService.register(this, dfd);
			

		} catch (Ice.LocalException e) {
			e.printStackTrace(); 
		
		} catch (FIPAException e) {
			e.printStackTrace();
			
		} catch (Exception e) { 
			e.printStackTrace();
		} 

		
	}
	
	/**
	 * Pre-death clean up called by JADE
	 */
	protected void takeDown() {
		log(DEBUG.WARNING, "Goodbye!");

		try {
			/**************************** ORCA PROXIES *****************************/
			// clean up Ice
			if (this.ic != null) { 
				this.ic.destroy(); 
			} 

			/************************** PROVIDED SERVICES **************************/
			DFService.deregister(this);
			
		} catch (FIPAException e) {
			// most likely, we just aren't registered yet. so fall through unless
			// we like spam
			if (this.debug_level == DEBUG.SPAM)
				e.printStackTrace();
			
		} catch (Exception e) { 
			e.printStackTrace();
		} 
	}

	// Internal methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Initialises Ice proxies and Orca interfaces
	 * @param robo_colour Colour of the robot used to construct interface names
	 */
	protected void initProxies(String robo_colour) {
		// connect to Ice
		this.ic = Ice.Util.initialize(DEFAULT_ARGS);
		
		// Interface to PathEvaluator (same for all robots)
		Ice.ObjectPrx patheval_base = this.ic.stringToProxy("pathevaluator@vm-ubuntu/pathevaluator");
		if (null == (this.patheval = PathEvaluatorPrxHelper.checkedCast(patheval_base)))
			throw new Error("Invalid proxy.");
	}

	/**
	 * Prints messages depending on debug level. Message is only printed if it's
	 * debug level is equal or higher than the one specified when constructing
	 * this agent.
	 * @param level Debug level of this message
	 * @param message
	 */
	protected void log(DEBUG level, String message) {
		if (level.compareTo(this.debug_level) >= 0) {
			System.out.println(getAID().getName() + " " + level.toString().toLowerCase() + " - " + message);
		}
	}

	/**
	 * FIXME: temporary test scaboozie for Orca interfaces
	 */
	protected float estimateDistance() {
		orca.Frame2d start = createFrame(-1, 0);
		
		Task2d[] tasks = new Task2d[4];
		tasks[0] = createTask( -9,  0);
	  tasks[1] = createTask( -1,  3);
	  tasks[2] = createTask( 23, -4);
	  tasks[3] = createTask(-23,  8);
	
		Task2d[] committed = new Task2d[1];
		committed[0] = createTask( 11,  0);

		PathEvaluatorTask task = new PathEvaluatorTask();
	  task.maxBundles 		= 15;
	  task.bundleSize 		= 3;
	  task.start 					= start;
	  task.committedTasks = committed;
	  task.newTasks 			= tasks;
	
		try {
		  this.patheval.setTask(task);
		} catch (orca.BusyException e) {
			log(DEBUG.ERROR, "Path evaluator is too busy for us :-(");
		} catch (orca.RequiredInterfaceFailedException e) {
			throw new Error(e.getMessage());
		}
		
		// FIXME: stupid way to wait
		Bundle2d[] result;
		while(true) {
			try {
				result = this.patheval.getData();
			} catch (Ice.UnknownUserException e) {
				continue;
			}
			break;
		}

	  return result[0].cost;
	}
	
	
	// Helper methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Helper function to create a talker.Task2d object
	 * @param x coordinate
	 * @param y coordinate
	 * @return talker.Task2d object
	 */
	private Task2d createTask(float x, float y) {
		return new Task2d(createFrame(x, y));
	}
	
	/**
	 * Helper function to create an orca.Frame2d object
	 * @param x coordinate
	 * @param y coordinate
	 * @return orca.Frame2d object
	 */
	private orca.Frame2d createFrame(float x, float y) {
		return new orca.Frame2d(new orca.CartesianPoint2d(x, y), 0.0);
	}
	
	/**
	 * Parses a message like "[0 1] [10.3 7]" and creates a list of Task2ds
	 * @param message String of format "[<x> <y>] [<x> <y>] ..."
	 * @return List of Task2ds that were embedded in the message
	 */
	private ArrayList<Task2d> extractTasks(String message) {
		ArrayList<Task2d> tasks = new ArrayList<Task2d>();
		Pattern pattern = Pattern.compile("\\[(\\-?\\d+[\\.\\d]*) (\\-?\\d+[\\.\\d]*)\\]");
		for (MatchResult r : findAll(pattern, message)) {
			tasks.add(createTask(Float.parseFloat(r.group(1)), Float.parseFloat(r.group(2))));
		}
		return tasks;
	}
	
	/**
	 * Wrapper to make Java's regex a little bit less ugly
	 * @param pattern Pattern used to take String apart
	 * @param text Text to be matched against a pattern
	 * @return List of results that could be matched
	 */
	private static List<MatchResult> findAll(Pattern pattern, String text) {
		List<MatchResult> results = new ArrayList<MatchResult>();
		Matcher m = pattern.matcher(text);
		while (m.find()) results.add(m.toMatchResult());
		return results;
	}
	
	//===========================================================================
	// Inner class definitions
	//===========================================================================
	
	// TickedBehaviour which keeps track of own tasks and actions
	private class RobotBehaviour extends TickerBehaviour {
		/**
		 * Constructor
		 * @param a 			Agent this behaviour belongs to
		 * @param period	Milliseconds between ticks
		 */
		public RobotBehaviour(Agent a, long period) {
			super(a, period);
		}
		
		/**
		 * Do stuff.
		 */
		protected void onTick() {
			if (bundle.tasks != null)
				log(DEBUG.SPAM, "RobotBehaviour: I am currently committed to " + bundle.tasks.length + " tasks, and have recently received " + received_tasks.size() + " tasks.");
			else
				log(DEBUG.SPAM, "RobotBehaviour: I am currently committed to no tasks, and have recently received " + received_tasks.size() + " tasks.");

			float distance = estimateDistance();
			log(DEBUG.SPAM, "RobotBehaviour: Distance estimate is " + distance);
		}
	}

	// TickedBehaviour which checks the DF every minute to update it's list of nearby agents
	private class UpdateNeighbourBehaviour extends TickerBehaviour {
		/**
		 * Constructor
		 * @param a 			Agent this behaviour belongs to
		 * @param period	Milliseconds between ticks
		 */
		public UpdateNeighbourBehaviour(Agent a, long period) {
			super(a, period);
		}
		
		/**
		 * Updates list of nearby agents
		 */
		protected void onTick() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("task-buying");
			template.addServices(sd);

			try {
				DFAgentDescription[] result = DFService.search(this.myAgent, template);
				buyer_robots.clear();
				for (DFAgentDescription d: result)
					buyer_robots.add(d.getName());
					log(DEBUG.INFO, "UpdateNeighbourBehaviour: I have " + result.length + " neighbours.");

			} catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}

	// CyclicBehaviour which listens for requests of for adding new goals
	private class TaskReceiverBehaviour extends CyclicBehaviour {
		private  MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		
		private TaskReceiverBehaviour(Agent a) {
			super(a);
		}
		
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// process request message
				log(DEBUG.INFO, "TaskReceiverBehaviour: received a request message.");
				String content = msg.getContent();
				log(DEBUG.SPAM, "TaskReceiverBehaviour: content is:\n" + content);

				for(Task2d t: extractTasks(content)) {
					received_tasks.add(t);
					log(DEBUG.SPAM, "x: " + t.target.p.x + " y: " + t.target.p.y);
				}
				
			} else {
				// block till we receive the next message
				block();
			}
		}
	}
}