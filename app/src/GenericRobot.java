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
	private final Double	 CLOSE_ENOUGH_EPSILON = 0.5;
	private final long		 GOAL_EVAL_TIMEOUT = 3000; // 3.0s
	private final int			 GOAL_EVAL_MAX_TRY_COUNT = 6;
	private final long		 LOCALISER_TIMEOUT = 500; // 0.5s
	private final String[] DEFAULT_ARGS = {"--Ice.Default.Locator=IceGrid/Locator:default -p 12000"};
	private final String   GOAL_COORDS	= "[-23 8] [-23 1] [-21 -4] [-17 7] [-17 -2] [-17 -5] [-14 7] [-12 -5] [-10 7] [-9 0] [-7 7] [-6 -5] [-5 -1] [-2 8.5] [-1 3] [-1 0] [2 0] [2 -8] <5.8 7> [8 -8] [10 7] [11 0] [11 -8] [14 7] [14 0] [14 -5] <17.5 8> <20 8> [23 -4] ";
	// TODO: there are probs with
	// 			 5.5 7 => 5.8 7
	//			 17 7 => 17.5 8
	// 			 20 7 => 20 8

	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	private ArrayList<AID> 			buyer_robots		= new ArrayList<AID>();
	private ArrayList<Task2d>		tasks_for_sale	= new ArrayList<Task2d>();
	private Bundle2d						bundle					= new Bundle2d();
	private DEBUG 							debug_level 		= DEBUG.INFO;
	private int									active_auction_counter	= 0;
	private orca.Frame2d				current_target	= null;

	// Orca interfaces
	private Ice.Communicator 		ic;
	private PathEvaluatorPrx 		patheval; // used to find best bundles
	private orca.Localise2dPrx	localiser; // used to get position of robot
	private orca.PathFollower2dPrx follower; // used to drive robot around

	// Agent life cycle methods
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
			
			// create random id
			Random r = new Random();
			int r1 = r.nextInt(10000) - 5000;
			int r2 = r.nextInt(10000) - 5000;
			
			// TickerBehaviour which keeps track of own tasks and actions
			addBehaviour(new RobotBehaviour(this, 5000));

			// TickerBehaviour which checks the DF every 30 secs to update it's list of nearby agents
			addBehaviour(new UpdateNeighbourBehaviour(this, 30000));
			
			// CyclicBehaviour which listens for requests of for adding new goals
			addBehaviour(new TaskReceiverBehaviour(this));
			
			// CyclicBehaviour which listens for CFP to try to buy new goals
			addBehaviour(new TriggerBuyerBehaviour(this));

			// TickerBehaviour which occasionally tries to sell it's own tasks
			addBehaviour(new TriggerSaleBehaviour(this, 10000 + r1));
			
			// TickerBehaviour that randomly adds some new tasks to my list
			addBehaviour(new RandomTaskGenerator(this, 90000 + r2));
			
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

	/**
	 * Initialises Ice proxies and Orca interfaces
	 * @param robo_colour Colour of the robot used to construct interface names
	 */
	protected void initProxies(String robo_colour) {
		// connect to Ice
		this.ic = Ice.Util.initialize(DEFAULT_ARGS);
		
		String patheval_name = "pathevaluator@vm-ubuntu/pathevaluator";
		String localiser_name = "localise2d@vm-ubuntu/" + robo_colour + ".simlocaliser";
		String follower_name = "pathfollower2d@vm-ubuntu/" + robo_colour + ".goalplanner";

		// Interface to PathEvaluator (same for all robots)
		try {
			Ice.ObjectPrx patheval_base = this.ic.stringToProxy(patheval_name);
			if (null == (this.patheval = PathEvaluatorPrxHelper.checkedCast(patheval_base)))
				throw new Error("Proxy could not be based.");
				
		} catch (Exception e) {
			log(DEBUG.ERROR, "initProxies: could not connect to goal evaluator component (" + patheval_name + ").");
			e.printStackTrace();
		}

		// Interface to Localiser (1 per robot)
		try {
			Ice.ObjectPrx localiser_base = this.ic.stringToProxy(localiser_name);
			if (null == (this.localiser = orca.Localise2dPrxHelper.checkedCast(localiser_base)))
				throw new Error("Proxy could not be based.");
			
		} catch (Exception e) {
			log(DEBUG.WARNING, "initProxies: could not connect to " + robo_colour + " localiser component (" + localiser_name + ").");
			e.printStackTrace();
		}

		// Interface to GoalPlanner, i.e. follower (1 per robot)
		try {
			Ice.ObjectPrx follower_base = this.ic.stringToProxy(follower_name);
			if (null == (this.follower = orca.PathFollower2dPrxHelper.checkedCast(follower_base)))
				throw new Error("Proxy could not be based.");

		} catch (Exception e) {
			log(DEBUG.WARNING, "initProxies: could not connect to " + robo_colour + " goal planner component (" + follower_name + ").");
			e.printStackTrace();
		}
	}

	// Internal methods
	/////////////////////////////////////////////////////////////////////////////

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

	protected void drive() {
		if (bundle.tasks != null && bundle.tasks.length > 0) {
			// check if I need to change my goal
			orca.Frame2d loc = getLocation();

			// update my bundle (and thus immediate goal) if necessary
			if (loc != null && closeEnough(loc, current_target)) {
				log(DEBUG.INFO, String.format("drive: I have arrived at my current target (x:%.1f, y:%.1f)", loc.p.x, loc.p.y));
				
				Bundle2d new_bundle = new Bundle2d();
				new_bundle.tasks = new Task2d[bundle.tasks.length - 1];
				for (int i = 0; i < new_bundle.tasks.length; i++) {
					new_bundle.tasks[i] = bundle.tasks[i+1];
				}

				Bundle2d[] bundles = getBundlesWithCosts(loc, new_bundle.tasks, new Task2d[0], 1, 1);
				this.bundle = bundles[0];
			}
			
			// update my current target if necessary
			if (this.current_target == null || !closeEnough(this.current_target, this.bundle.tasks[0].target)) {
				this.current_target = this.bundle.tasks[0].target;
				log(DEBUG.INFO, String.format("drive: Heading to new target at x:%.1f, y:%.1f", current_target.p.x, current_target.p.y));
				
				// drive to target
				orca.Waypoint2d waypoint = new orca.Waypoint2d();
				waypoint.target = current_target;
				orca.Waypoint2d[] path = {waypoint};
				orca.PathFollower2dData target = new orca.PathFollower2dData();
				target.path = path;
				try {
					follower.setData(target, true);
				} catch (Exception e) {
					log(DEBUG.ERROR, "drive: Path follower does not like our new target.");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Calls GoalEvaluator component to create bundle bids
	 * @param tasks ArrayList of Tasks2d for which the bids are to be created
	 * @return Array of Bundle2ds
	 */
	protected Bundle2d[] getBundlesWithCosts(ArrayList<Task2d> tasks, boolean relative_costs) {
		// simplify, i.e. remove all tasks that i am already committed to
		for (int i = tasks.size() - 1; i >= 0; i--) {
			if (getTaskIndex(tasks.get(i), bundle.tasks) != -1) {
				log(DEBUG.NOTE, "getBundlesWithCosts: I already own task x:" + tasks.get(i).target.p.x + ", y:" + tasks.get(i).target.p.y);
				tasks.remove(i);
			}
		}
		
		// return my current bundle with cost 0 if no tasks left
		if (tasks.size() == 0) {
			log(DEBUG.NOTE, "getBundlesWithCosts: Easy win. I already own all tasks.");
			Bundle2d current_bundle = bundle;
			current_bundle.cost = 0;
			Bundle2d[] bundles = { current_bundle };
			return bundles;
		}
		
		// get my location
		orca.Frame2d start = getLocation();
		if (start == null) {
			log(DEBUG.WARNING, "getBundlesWithCosts: could not retrieve my location.");
			return new Bundle2d[0];
		}

/*
		Task2d[] tasks = new Task2d[4];
		tasks[0] = createTask( -9,  0);
	  tasks[1] = createTask( -1,  3);
	  tasks[2] = createTask( 23, -4);
	  tasks[3] = createTask(-23,  8);
	
		Task2d[] committed = new Task2d[1];
		committed[0] = createTask( 11,  0);
*/

		Bundle2d[] bundles = getBundlesWithCosts(	start,
																							this.bundle.tasks, 
																							(Task2d[]) tasks.toArray(new Task2d[tasks.size()]),
																							BUNDLE_SIZE,
																							MAX_BUNDLES);
		
		// adjust costs for bundles by withdrawing my current cost
		// i.e. bid only increase in costs not total costs
		if (relative_costs) {
			for(int i = 0; i < bundles.length; i++)
				bundles[i].cost -= this.bundle.cost;
		}

	  return bundles;
	}
	
	protected Bundle2d[] getBundlesWithCosts(orca.Frame2d start, Task2d[] committed, Task2d[] new_tasks, int bundle_size, int max_bundles) {
		// set up task for goal evaluator
		PathEvaluatorTask task = new PathEvaluatorTask();
	  task.maxBundles 		= max_bundles;
	  task.bundleSize 		= bundle_size;
	  task.start 					= start;
	  task.newTasks 			= new_tasks;
	  task.committedTasks = committed;

		// create random id
		Random r = new Random();
		int some_number = r.nextInt(100000);
		task.id							= getAID().getName() + "-" + some_number;

		// run goal evaluator and give it a couple of chances
		PathEvaluatorResult result = null;

		for (int i = 0; i < GOAL_EVAL_MAX_TRY_COUNT; i++) {
			// set tasks anew on every second run
			if (i % 2 == 0) {
				try {
				  this.patheval.setTask(task);
				} catch (Ice.TimeoutException e) {
					log(DEBUG.ERROR, "getBundlesWithCosts: Goal evaluator timed out.");
					break;
				} catch (orca.BusyException e) {
					log(DEBUG.ERROR, "getBundlesWithCosts: Goal evaluator is too busy for us :-(");
					break;
				} catch (orca.RequiredInterfaceFailedException e) {
					throw new Error(e.getMessage());
				}
			}

			// wait a while for computation
			boolean keep_looking = true;
			long start_time = Calendar.getInstance().getTimeInMillis();
			long now = start_time;

			while(keep_looking) {
				try {
					result = this.patheval.getData();
				} catch (Ice.UnknownUserException e) {
					continue;
/*
				} catch (orca.DataNotExistException e) {
					log(DEBUG.WARNING, "getBundlesWithCosts: Goal Evaluator did not find result.");
					return new Bundle2d[0];
*/
				}

				// we might be lucky
				if (result != null) {
					now = Calendar.getInstance().getTimeInMillis();
					if (result.id.equals(task.id) || now - start_time > GOAL_EVAL_TIMEOUT)
						keep_looking = false;
				}
			}

			if (result.id.equals(task.id)) {
				log(DEBUG.NOTE, String.format("getBundlesWithCosts: Goal evaluator returned result in %.1fs.", (now - start_time) / 1000.0));
				break;

			} else {
				int wait_for = r.nextInt(5000) + 2500;
				log(DEBUG.SPAM, String.format("getBundlesWithCosts: try %d to contact goal evaluator failed. retrying in %.1fs...", i + 1, wait_for / 1000.0));

				try {
					Thread.sleep(wait_for);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		if (result != null) {
			log(DEBUG.SPAM, "getBundlesWithCosts: result id is " + result.id + ", I want: " + task.id);
		}

		if (result != null && result.id.equals(task.id)) {
			return result.data;
		} else {
			log(DEBUG.ERROR, String.format("getBundlesWithCosts: Goal evaluator did not return any results within %.1fs. Something must be wrong.", GOAL_EVAL_TIMEOUT / 1000.0));
			return new Bundle2d[0];
		}
	}
	
	protected orca.Frame2d getLocation() {
		orca.Localise2dData data = null;
		orca.Frame2d location = null;
		
		if (this.localiser != null) {
			// get location of robot
			boolean keep_looking = true;
			long start_time = Calendar.getInstance().getTimeInMillis();
			long now = start_time;

			while(keep_looking) {
				try {
					data = this.localiser.getData();
				} catch (Ice.UnknownUserException e) {
					continue;
				} catch (orca.DataNotExistException e) {
					log(DEBUG.WARNING, "getLocation: localiser returned DataNotExistException");
					return null;
				}

				// we might be lucky
				if (data != null) {
					now = Calendar.getInstance().getTimeInMillis();
					if (now - start_time > LOCALISER_TIMEOUT)
						keep_looking = false;
				}
			}

			if (data != null && data.hypotheses.length != 0) {
				// use value from localiser
				location = data.hypotheses[0].mean;
				log(DEBUG.NOTE, "getLocation: my location is x:" + location.p.x + " y:" + location.p.y);
			}			

		} else {
			// use default value for debugging if no data found (print warning in that case)
			location = createFrame(-1, 0);
			log(DEBUG.WARNING, "getLocation: could not read my location, using dummy value.");
		}
			

		return location;
	}
	
	/**
	 * Commits agent to a new bundle, which has to include all previously
	 * committed tasks
	 * @param new_bundle
	 */
	protected void setNewBundles(ArrayList<Bundle2d> new_bundles) {
		if (new_bundles.size() != 1) {
			log(DEBUG.WARNING, "TaskBuyerBehaviour::handleAcceptProposal: we are getting " + new_bundles.size() + " bundles assigned at once, which should never be done or synergies between my bundles won't properly be accounted for.");

			// TODO: actually implement this and change level to DEBUG.INFO
			log(DEBUG.WARNING, "TaskBuyerBehaviour::handleAcceptProposal: reselling assigned tasks. IMPLEMENT THIS!");

			return;
		}

		// FIXME: we can't properly handle multiple auctions at once this way
		// TODO: add better way for figuring out whether we need to do recalculations or not
		//       e.g. check if every tasks in our bundle.tasks is in the new bundle we
		Bundle2d new_bundle = new_bundles.get(0);
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
	}
	
	/**
	 * Selects winning bundles from a bunch of received bids
	 * @param bids HashMap of 1 sender => n bundles
	 * @return HashMap of 1 sender => 1 assigned bundle
	 */
	protected HashMap<ACLMessage, Bundle2d> selectWinningBundles(HashMap< ACLMessage, ArrayList<Bundle2d> > bids) {
		// process bundles and decide on which tasks to assign

		// TODO: generalise for handling multiple winning bundles
		// FIXME: for starters, use simple SSI auction
		Bundle2d 		best_bundle = null;
		float  			best_cost = Float.POSITIVE_INFINITY;
		ACLMessage	best_offer = null;
		
		// iterate over all agents
		Set< Map.Entry< ACLMessage, ArrayList<Bundle2d> > > set = bids.entrySet();
		for (Map.Entry< ACLMessage, ArrayList<Bundle2d> > me : set) {
			ACLMessage key = me.getKey();
			log(DEBUG.SPAM, "selectWinningBundles: checking bids from " + key.getSender().getName() + ", who has " + bids.get(key).size() + " bids.");
			
			// iterate over all bundles in this package
			for (Bundle2d bundle: me.getValue()) {
				// skip bundle if not verified
				ArrayList<Integer> task_indices = getTaskForSaleIndex(bundle);
				if (task_indices.size() == 0) {
					// TODO: punish agent
					log(DEBUG.WARNING, "selectWinningBundles: Received invalid bid from " + key.getSender().getName());

					// skip this bid
					continue;
				}
				
				if (bundle.cost < best_cost) {
					best_bundle = bundle;
					best_cost 	= bundle.cost;
					best_offer	= key;
				}
			}
		}
		
		HashMap<ACLMessage, Bundle2d> winners = new HashMap<ACLMessage, Bundle2d>();

		// TODO: generalise for handling multiple bundles
		if (best_bundle != null && best_bundle.tasks != null) {
			winners.put(best_offer, best_bundle);

			// remove tasks from for-sale list
			// TODO: should probably not be done here in case message not delivered?!
			ArrayList<Integer> task_indices;
			while ((task_indices = getTaskForSaleIndex(best_bundle)) != null && task_indices.size() > 0) {
				int i = task_indices.get(0).intValue();
				tasks_for_sale.remove(i);
				log(DEBUG.SPAM, String.format("selectWinningBundles: task #%d to %s - %d tasks still for sale.", i, best_offer.getSender().getName(), tasks_for_sale.size()));
			}

		}
		
		return winners;
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
	private boolean closeEnough(orca.Frame2d a, orca.Frame2d b) {
		return (closeEnough(a.p.x, b.p.x) && closeEnough(a.p.y, b.p.y));
		
	}
	private boolean closeEnough(Task2d a, Task2d b) {
		return closeEnough(a.target, b.target);
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
	
	/**
	 * find index of  the tasks that are close enough in the original tasks that are to sale
	 * functions as verification that we only sell tasks that we actually have for sale
	 * @param Either a single needle or a bundle
	 * @return Either a single index (or -1 if none found) or a list of indices (or empty list)
	 */
	private int getTaskForSaleIndex(Task2d needle) {
		Task2d[] haystack = (Task2d[]) tasks_for_sale.toArray(new Task2d[tasks_for_sale.size()]);
		return getTaskIndex(needle, haystack);
	}	
	private ArrayList<Integer> getTaskForSaleIndex(Bundle2d bundle) {
		ArrayList<Integer> indices = new ArrayList<Integer>();

		// check every task in the bundle
		for (Task2d t: bundle.tasks) {
			int i = getTaskForSaleIndex(t);
			if (i != -1)
				indices.add(new Integer(i));
		}

		// return valid task indices
		return indices;
	}	
	
	/**
	 * find index of a task in haystack that is close enough to the needle
	 * @param needle Tasks for which a close one will be looked for
	 * @param haystack List of tasks to look in
	 * @return Index of similar tasks to needle in haystack or -1 if none found
	 */
	private int getTaskIndex(Task2d needle, Task2d[] haystack) {
		for (int i = 0; i < haystack.length; i++) {
			Task2d for_sale = haystack[i];
			if (closeEnough(for_sale, needle))
				return i;
		}
		return -1;
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
			// add random tasks to my tasks for sale list
			Random 	r = new Random();
			
			int 		num = r.nextInt(5) + 2;
			for (int i = 0; i < num; i++ ) {
				int 		k = r.nextInt(all_possible_tasks.size());
				Task2d 	t = all_possible_tasks.get(k);

				if (getTaskIndex(t, bundle.tasks) == -1 && getTaskForSaleIndex(t) == -1) {
					log(DEBUG.INFO, "RandomTaskGenerator: drawn random task (#" + k + ") x:" + t.target.p.x + " y:" + t.target.p.y);
					tasks_for_sale.add(t);
				}
				else
					log(DEBUG.NOTE, "RandomTaskGenerator: not adding duplicate task (#" + k + ") x:" + t.target.p.x + " y:" + t.target.p.y);				
			}
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
			// some debugging blah blah
			if (bundle.tasks != null)
				log(DEBUG.NOTE, "RobotBehaviour: I am currently committed to " + bundle.tasks.length + " tasks, and have " + tasks_for_sale.size() + " tasks for sale.");
			else
				log(DEBUG.NOTE, "RobotBehaviour: I am currently committed to no tasks, and have " + tasks_for_sale.size() + " tasks for sale.");
			
			// handle driver
			drive();
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
			
			// get all bundles with relative costs
			Bundle2d[] bids = getBundlesWithCosts(tasks, true);
			
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
			
			setNewBundles(bundles);
			
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
			
			HashMap<ACLMessage, Bundle2d> winners = selectWinningBundles(bids);
			
			// send accept-proposals
			Set< Map.Entry<ACLMessage, Bundle2d> > set = winners.entrySet();
			for (Map.Entry<ACLMessage, Bundle2d> me : set) {
				ACLMessage offer = me.getKey();
				ACLMessage accept = offer.createReply();
				accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
				Bundle2d[] accepted_bundles = { me.getValue() };
				String bundle_string = createBundleString(accepted_bundles);
				accept.setContent(bundle_string);
				acceptances.add(accept);

				log(DEBUG.INFO, String.format("TaskSellerBehaviour::handleAllResponses: Selling the following bundle to " + offer.getSender().getName() + ":\n" + bundle_string));
			}
			
			// done
			active_auction_counter = 0;
			// TODO: remove myself from behaviours?
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