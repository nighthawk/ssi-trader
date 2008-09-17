/***
 * @author Adrian Schoenig, UNSW CSE, adrian.schoenig@gmail.com
 * @since 2008/07
 *
 * This JADE agent is modelling a robot which is implemented using the component-based
 * Orca framework. It requires a GoalEvaluator and a GoalPlanner component.
 *
 * Behaviours of this agent
 * - TickerBehaviour which checks the DF every minute to update it's list of nearby robots
 * - TODO: TickerBehaviour which keeps track of own tasks and actions
 * - CyclicBehaviour which listens for requests for adding new tasks
 * - TODO: ContractNetResponder which listens for CFP to try to buy new goals
 * - TODO: ContractNetInitiator triggered by TickerBehaviour to occasionally sell it's own tasks
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
import jade.proto.*;

import talker.*;

public class GenericRobot extends Agent {
	// Constants
	/////////////////////////////////////////////////////////////////////////////
	enum DEBUG { SPAM, NOTE, INFO, WARNING, ERROR };

	// TODO: these should probably be set some different way, e.g. config file
	private final int			 MAX_WAIT_CYCLE_FOR_AUCTION_START = 5;
	private final int			 MAX_BUNDLES = 25;
	private final int			 BUNDLE_SIZE = 1;
	private final Double	 CLOSE_ENOUGH_EPSILON = 0.1;
	private final String[] DEFAULT_ARGS = {"--Ice.Default.Locator=IceGrid/Locator:default -p 12000"};
	private final String   GOAL_COORDS	= "[-23 8] [-23 1] [-21 -4] [-17 7] [-17 -2] [-17 -5] [-14 7] [-12 -5] [-10 7] [-9 0] [-7 7] [-6 -5] [-5 -1] [-2 8.5] [-1 3] [-1 0] [2 0] [2 -8] [5.8 7] [8 -8] [10 7] [11 0] [11 -8] [14 7] [14 0] [14 -5] [17 8] [20 8] [23 -4] ";
	// TODO: there are probs with
	// 			 5.5 7 => 5.8 7
	//			 17 7 => 17 8
	// 			 20 7 => 20 8

	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	private ArrayList<AID> 		buyer_robots		= new ArrayList<AID>();
	private ArrayList<Task2d>	tasks_for_sale	= new ArrayList<Task2d>();
	private Bundle2d					bundle					= new Bundle2d();
	private DEBUG 						debug_level 		= DEBUG.INFO;
	private int								active_auction_counter	= 0;

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
		
		// initialise values
		String robo_colour = (String) args[0];
		this.debug_level = DEBUG.valueOf(((String) args[args.length - 1]).toUpperCase());
		this.bundle.tasks = new Task2d[0];
		this.bundle.cost  = 0;
		
		try {
 			/**************************** ORCA PROXIES *****************************/
			log(DEBUG.NOTE, "Connecting to Ice.");
			initProxies(robo_colour);
			
 			/************************** ADDING BEHAVIOURS **************************/
			log(DEBUG.NOTE, "Adding behaviours.");
			
			// TickerBehaviour which keeps track of own tasks and actions
			addBehaviour(new RobotBehaviour(this, 30000));

			// TickerBehaviour which checks the DF every 30 secs to update it's list of nearby agents
			addBehaviour(new UpdateNeighbourBehaviour(this, 30000));
			
			// CyclicBehaviour which listens for requests of for adding new goals
			addBehaviour(new TaskReceiverBehaviour(this));
			
			// CyclicBehaviour which listens for CFP to try to buy new goals
			addBehaviour(new TriggerBuyerBehaviour(this));

			// TickerBehaviour which occasionally tries to sell it's own tasks
			addBehaviour(new TriggerSaleBehaviour(this, 15000));
			
			// TickerBehaviour that randomly adds some new tasks to my list
			addBehaviour(new RandomTaskGenerator(this, 60000));
			
 			/************************** PROVIDED SERVICES **************************/
			log(DEBUG.NOTE, "Registering with DF.");

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
	 * Calls GoalEvaluator component to create bundle bids
	 * @param tasks ArrayList of Tasks2d for which the bids are to be created
	 * @return Array of Bundle2ds
	 */
	protected Bundle2d[] getBundlesWithCosts(ArrayList<Task2d> tasks) {
		// TODO: this should get the actual location of the robot
		//       or a default value for debugging (print warning in that case)
		log(DEBUG.WARNING, "estimateDistance: could not read my location, using dummy value.");
		orca.Frame2d start = createFrame(-1, 0);
		
/*
		Task2d[] tasks = new Task2d[4];
		tasks[0] = createTask( -9,  0);
	  tasks[1] = createTask( -1,  3);
	  tasks[2] = createTask( 23, -4);
	  tasks[3] = createTask(-23,  8);
	
		Task2d[] committed = new Task2d[1];
		committed[0] = createTask( 11,  0);
*/

		// set up task for goal evaluator
		PathEvaluatorTask task = new PathEvaluatorTask();
	  task.maxBundles 		= MAX_BUNDLES;
	  task.bundleSize 		= BUNDLE_SIZE;
	  task.start 					= start;
	  task.newTasks 			= (Task2d[]) tasks.toArray(new Task2d[tasks.size()]);
	  task.committedTasks = this.bundle.tasks;
	
		try {
		  this.patheval.setTask(task);
		
		} catch (Ice.TimeoutException e) {
			log(DEBUG.ERROR, "Path evaluator timed out.");
		
		} catch (orca.BusyException e) {
			log(DEBUG.ERROR, "Path evaluator is too busy for us :-(");
			
		} catch (orca.RequiredInterfaceFailedException e) {
			throw new Error(e.getMessage());
		}
		
		// FIXME: stupid way to wait and doesn't guarantee i'm getting _my_ data
		Bundle2d[] result;
		while(true) {
			try {
				result = this.patheval.getData();
			} catch (Ice.UnknownUserException e) {
				continue;
			}
			break;
		}
		
		// adjust costs for bundles by withdrawing my current cost
		// i.e. bid only increase in costs not total costs
		for(int i = 0; i < result.length; i++) {
			result[i].cost -= this.bundle.cost;
		} 

	  return result;
	}
	
	/**
	 * Checks whether all tasks in my currently committed bundle are actually
	 * in my new_bundle. If not: either trigger error or add not-included
	 * tasks to my for-sale list
	 * @param new_bundle New bundle that I get assigned to
	 * @return Whether my currently committed bundle is a subset of the new bundle
	 */
	protected boolean validateBundleConsistency(Bundle2d new_bundle) {
		// TODO: implement this
		return true;
	}
	
	
	// Helper methods
	/////////////////////////////////////////////////////////////////////////////

	private <T> ArrayList<T> arrayToArrayList(T[] array) {
		ArrayList<T> al = new ArrayList<T>();
		for(T elem: array) {
			al.add(elem);
		}
		return al;
	}
	
	private boolean closeEnough(double a, double b) {
		return Math.abs(a - b) < CLOSE_ENOUGH_EPSILON;
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
	 * Helper function to create a talker.Task2d object
	 * @param x coordinate
	 * @param y coordinate
	 * @return talker.Task2d object
	 */
	private Task2d createTask(float x, float y) {
		return new Task2d(createFrame(x, y));
	}
	
	/**
	 * Creates a message like "[0 1] [10.3 7]" from a list of Task2ds
	 * @param tasks ArrayList of Task2ds
	 * @param String of format "[<x> <y>] [<x> <y>] ..."
	 */
	private String createTaskString(ArrayList<Task2d> tasks) {
		String output = "";
		for (Task2d t: tasks)
			output += String.format("[%f %f] ", t.target.p.x, t.target.p.y);
		return output;
	}
	
	/**
	 * Creates a message with lines like "56.4: [0 1] [10.3 7]"
	 * @param bundles Array of Bundle2ds
	 * @return String of format "<cost>: <tasks>\n<cost>: <tasks>\n..."
	 */
	private String createBundleString(Bundle2d[] bundles) {
		String output = "";
		for (Bundle2d b: bundles) {
			output += b.cost + ": " + createTaskString(arrayToArrayList(b.tasks)) + "\n";
		}
		return output;
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
	 * Parses a message with lines like "56.4: [0 1] [10.3 7]" and creates a list
	 * of Bundle2ds
	 * @param message String of format "<cost>: <tasks>\n<cost>: <tasks>\n..."
	 * @return List of Bundle2ds that were embedded in the  message
	 */
	private ArrayList<Bundle2d> extractBundles(String message) {
		ArrayList<Bundle2d> bundles = new ArrayList<Bundle2d>();

		// each line represents one bundle
		String[] result = "this is a test".split("\\s");
		for (String line: message.split("\\n")) {
			log(DEBUG.SPAM, "extractBundles: analysing\n" + line);
			Pattern pattern = Pattern.compile("^(\\-?\\d+[\\.\\d]*):(.*)");
			for (MatchResult r : findAll(pattern, line)) {
				ArrayList<Task2d> tasks = extractTasks(r.group(2));
				bundles.add(new Bundle2d(Float.parseFloat(r.group(1)), (Task2d[]) tasks.toArray(new Task2d[tasks.size()])));
			}
		}
		return bundles;
	}
	
	/**
	 * Wrapper to make Java's regex a little bit less ugly
	 * @param pattern Pattern used to take String apart
	 * @param text Text to be matched against a pattern
	 * @return List of results that could be matched
	 */
	private List<MatchResult> findAll(Pattern pattern, String text) {
		List<MatchResult> results = new ArrayList<MatchResult>();
		Matcher m = pattern.matcher(text);
		while (m.find()) results.add(m.toMatchResult());
		return results;
	}
	
	//===========================================================================
	// Inner class definitions
	//===========================================================================
	
	// TickerBehaviour that adds a random tasks
	private class RandomTaskGenerator extends TickerBehaviour {
		ArrayList<Task2d> all_possible_tasks;
		
		public RandomTaskGenerator(Agent a, long period) {
			super(a, period);
			
			all_possible_tasks = extractTasks(GOAL_COORDS);
			log(DEBUG.SPAM, "RandomTaskGenerator: loaded " + all_possible_tasks.size() + " possible tasks.");
			onTick();
		}
		
		protected void onTick() {
			// add random task to my tasks for sale list
			Random 	r = new Random();
			int 		i = r.nextInt(all_possible_tasks.size());
			Task2d 	t = all_possible_tasks.get(i);
			log(DEBUG.INFO, "RandomTaskGenerator: drawn random task (#" + i + ") x:" + t.target.p.x + " y:" + t.target.p.y);
			tasks_for_sale.add(t);
		}
	}
	
	// TickerBehaviour which keeps track of own tasks and actions
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
				log(DEBUG.NOTE, "RobotBehaviour: I am currently committed to " + bundle.tasks.length + " tasks, and have " + tasks_for_sale.size() + " tasks for sale.");
			else
				log(DEBUG.NOTE, "RobotBehaviour: I am currently committed to no tasks, and have " + tasks_for_sale.size() + " tasks for sale.");
		}
	}

	// Single session ContractNetResponder, allowing us to participate
	// in multiple auctions at a time
	private class TaskBuyerBehaviour extends SSContractNetResponder {
		private TaskBuyerBehaviour(Agent a, ACLMessage cfp) {
			super(a, cfp);
		}
		
		protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, NotUnderstoodException {
			// extract tasks from message and calculate costs to get there
			ArrayList<Task2d> tasks = extractTasks(cfp.getContent());
			log(DEBUG.SPAM, "TaskBuyerBehaviour::handleCfp: Received CFP with " + tasks.size() + " tasks.");
			if (tasks.size() == 0)
				throw new NotUnderstoodException("There are no tasks in this CFP.");
			
			Bundle2d[] bids = getBundlesWithCosts(tasks);
			
			ACLMessage response;
			if(bids.length > 0) {
				response = new ACLMessage(ACLMessage.PROPOSE);
				response.setContent(createBundleString(bids));
				response.setProtocol("fipa-contract-net");
				
			} else {
				log(DEBUG.SPAM, "TaskBuyerBehaviour::handleCfp: no bundles for me to bid on.");
				throw new RefuseException("No bundles for me to bid on.");
			}
			
			return response;
		}

		protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
			// extract tasks from accept-proposal message
			ArrayList<Bundle2d> bundles = extractBundles(accept.getContent());
			
			if (bundles.size() != 1) {
				log(DEBUG.WARNING, "TaskBuyerBehaviour::handleAcceptProposal: we are getting " + bundles.size() + " bundles assigned at once, which should never be done or synergies between my bundles won't properly be accounted for.");
				
				// TODO: actually implement this and change level to DEBUG.INFO
				log(DEBUG.WARNING, "TaskBuyerBehaviour::handleAcceptProposal: reselling assigned tasks. IMPLEMENT THIS!");
				
				return null;
			}
			
			// FIXME: we can't properly handle multiple auctions at once this way
			// TODO: add better way for figuring out whether we need to do recalculations or not
			//       e.g. check if every tasks in our bundle.tasks is in the new bundle we
			Bundle2d new_bundle = bundles.get(0);
			if (validateBundleConsistency(new_bundle)) {

				int diff_count = new_bundle.tasks.length - bundle.tasks.length;
				float diff_cost  = new_bundle.cost;
				new_bundle.cost += bundle.cost;	// sum up costs
				log(DEBUG.INFO, String.format("TaskBuyerBehaviour::handleAcceptProposal: bought %d new tasks for cost of %.2f.", diff_count, diff_cost));
				bundle = new_bundle;
				
			} else {

				// TODO: implement this properly
/*				
				// set my new bundle
				ArrayList<Task2d> tasks = arrayToArrayList(bundle.tasks);
				int new_count = 0;
				for (Bundle2d b: bundles) {
					for (Task2d t: b.tasks) {
						tasks.add(t);
						new_count++;
					}
				}
				bundle.tasks = tasks.toArray(new Task2d[tasks.size()]);
				log(DEBUG.NOTE, "TaskBuyerBehaviour::handleAcceptProposal: added " + new_count + " new tasks.");

				// TODO: call goal evaluator to re-sort committed tasks
*/
			}
			
			// no message sent back
			return null;
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
				log(DEBUG.NOTE, "TaskReceiverBehaviour: received a request message.");
				String content = msg.getContent();
				log(DEBUG.SPAM, "TaskReceiverBehaviour: content is:\n" + content);

				for(Task2d t: extractTasks(content)) {
					tasks_for_sale.add(t);
					log(DEBUG.SPAM, "x: " + t.target.p.x + " y: " + t.target.p.y);
				}
				
			} else {
				// block till we receive the next message
				block();
			}
		}
	}

	// ContractNetInitiator, which sells tasks 
	private class TaskSellerBehaviour extends ContractNetInitiator {
		ArrayList<Task2d> tasks;
		
		public TaskSellerBehaviour(ArrayList<Task2d> t) {
			super(null, null);
		}
		
		/**
		 * Prepare call for proposal will be send to all agents
		 * @param cfp Empty ACLMessage which will be filled
		 * @return Vector of CFPs, though we'll only need one
		 */
		protected Vector<ACLMessage> prepareCfps(ACLMessage cfp) {
			// create CFP message with tasks as content and send out to
			// all buyer agents
			// TODO: message could also include some information on bidding
			//       scheme for verification, e.g. bundle size and stuff
			cfp = new ACLMessage(ACLMessage.CFP);
			String message = createTaskString(tasks_for_sale);
			cfp.setContent(message);
			cfp.setProtocol("fipa-contract-net");
			log(DEBUG.SPAM, "TaskSellerBehaviour::prepareCfps: message for buyers is:\n" + message);
			for (AID r: buyer_robots) {
				cfp.addReceiver(r);
			}
			
			// we could send a bunch of CFPs at once, e.g. for sending
			// out customised messages to each agent
			Vector<ACLMessage> v = new Vector<ACLMessage>();
			v.add(cfp);
			return v;
		}
		
		/**
		 * 
		 */
		protected void handleAllResponses(Vector responses,
																			Vector acceptances)
		{
			HashMap< ACLMessage, ArrayList<Bundle2d> > bids = new HashMap< ACLMessage, ArrayList<Bundle2d> >();
			
			// extract bundles from all proposal messages
			// (silently ignoring not-understoods and refusals)
			for (int i = 0; i < responses.size(); i++) {
				ACLMessage r = (ACLMessage) responses.get(i);
				if (r.getPerformative() == ACLMessage.PROPOSE) {
					bids.put(r, extractBundles(r.getContent()));
				}
			}
			log(DEBUG.NOTE, "TaskSellerBehaviour::handleAllResponses: received bids from " + bids.size() + " agents.");
			
			// TODO: process bundles and decide on which tasks to assign
			// for starters, use simple SSI auction
			Bundle2d 		best_bundle = null;
			float  			best_cost = Float.POSITIVE_INFINITY;
			ACLMessage	best_offer = null;
			
			// iterate over all agents
			for (ACLMessage key: bids.keySet()) {
				log(DEBUG.SPAM, "TaskSellerBehaviour::handleAllResponses: checking bids from " + key.getSender().getName() + ", who has " + bids.get(key).size() + " bids.");
				
				// iterate over all bundles in this package
				for (Bundle2d bundle: bids.get(key)) {
					if (bundle.cost < best_cost) {
						best_bundle = bundle;
						best_cost 	= bundle.cost;
						best_offer	= key;
					}
				}
			}
			
			// find the tasks that are close enough in the original tasks that are to sale
			// functions as verification that we only sell tasks that we actually have for sale
			ArrayList<Integer> task_indices = verifyBundle(best_bundle);
			if (task_indices.size() > 0) {
				// send accept-proposals
				ACLMessage 				accept = best_offer.createReply();
				accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				Bundle2d[] accepted_bundles = {best_bundle};
				String bundle_string = createBundleString(accepted_bundles);
				accept.setContent(createBundleString(accepted_bundles));
				acceptances.add(accept);
				
				log(DEBUG.INFO, String.format("TaskSellerBehaviour::handleAllResponses: Selling the following bundle to " + best_offer.getSender().getName() + ":\n" + bundle_string));

				// remove task from the tasks that i have for sale
				for (int i = 0; i < task_indices.size(); i++) {
					int index = task_indices.get(i).intValue();
					tasks_for_sale.remove(index);
					log(DEBUG.SPAM, "TaskSellerBehaviour::handleAllResponses: sold #" + index + ", " + tasks_for_sale.size() + " tasks still for sale.");
				}

			} else {
				log(DEBUG.WARNING, "TaskSellerBehaviour::handleAllResponses: Received invalid bid from " + best_offer.getSender().getName());
				// TODO: punish agent
			}
			
			// done
			active_auction_counter = 0;
			// TODO: remove myself from behaviours?
		}
		
		/**
		 * Check if the task is actually for sale by me
		 */
		private ArrayList<Integer> verifyBundle(Bundle2d bundle) {
			ArrayList<Integer> indices = new ArrayList<Integer>();
			
			// check every task in the bundle
			for (Task2d t: bundle.tasks) {
				for (int i = 0; i < tasks_for_sale.size(); i++) {
					Task2d for_sale = tasks_for_sale.get(i);
					if (closeEnough(for_sale.target.p.x, t.target.p.x)
							&& closeEnough(for_sale.target.p.y, t.target.p.y))
						indices.add(new Integer(i));
				}
			}
			
			// only return indices if every task in the bundle is alright
			return indices;
		}
	}
	
	// CyclicBehaviour which listens for CFPs to start the TaskBuyerBehaviour
	private class TriggerBuyerBehaviour extends CyclicBehaviour {
		private  MessageTemplate mt = MessageTemplate.and(
																		MessageTemplate.MatchProtocol("fipa-contract-net"),
																		MessageTemplate.MatchPerformative(ACLMessage.CFP)
																	);
		
		private TriggerBuyerBehaviour(Agent a) {
			super(a);
		}
		
		public void action() {
			ACLMessage cfp = myAgent.receive(mt);
			if (cfp != null) {
				// process request message
				log(DEBUG.NOTE, "TriggerBuyerBehaviour: received a CFP message. Starting buyer behaviour.");
				myAgent.addBehaviour(new TaskBuyerBehaviour(myAgent, cfp));
				
			} else {
				// block till we receive the next message
				block();
			}
		}		
	}
	
	// TickerBehaviour which occasionally tries to start the TaskSellerBehaviour
	private class TriggerSaleBehaviour extends TickerBehaviour {
		/**
		 * Constructor
		 * @param a 			Agent this behaviour belongs to
		 * @param period	Milliseconds between ticks
		 */
		public TriggerSaleBehaviour(Agent a, long period) {
			super(a, period);
		}
		
		protected void onTick() {
			if (buyer_robots.size() == 0) {
				log(DEBUG.WARNING, "TriggerSaleBehaviour: there are no robots to sell my tasks to.");
			}
			else if (--active_auction_counter < 0 && tasks_for_sale.size() > 0) {
				log(DEBUG.SPAM, "TriggerSaleBehaviour: starting TaskSaleBehaviour");
				active_auction_counter = MAX_WAIT_CYCLE_FOR_AUCTION_START;
				myAgent.addBehaviour(new TaskSellerBehaviour(tasks_for_sale));
			
			}
			else if (active_auction_counter >= 0 && tasks_for_sale.size() > 0) {
				log(DEBUG.SPAM, "TriggerSaleBehaviour: can't start new auction, as old auction still busy (counter: " + active_auction_counter + ")");
			}
			else {
				active_auction_counter = 0;
			}
		}
	}
	
	// TickerBehaviour which checks the DF to update it's list of nearby agents
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
			
			int prev = buyer_robots.size();

			try {
				DFAgentDescription[] result = DFService.search(this.myAgent, template);
				buyer_robots.clear();
				for (DFAgentDescription d: result)
					//if (!d.getName().getName().equals(this.myAgent.getAID().getName()))
						buyer_robots.add(d.getName());
					
				log(DEBUG.SPAM, "UpdateNeighbourBehaviour: I have " + result.length + " neighbours.");
				int diff = buyer_robots.size() - prev;
				if (diff < 0)
					log(DEBUG.INFO, "UpdateNeighbourBehaviour: Lost contact to " + Math.abs(diff) + " agent(s).");
				else if (diff > 0)
					log(DEBUG.INFO, "UpdateNeighbourBehaviour: Got contact to  " + diff + " new agent(s).");

			} catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}

}