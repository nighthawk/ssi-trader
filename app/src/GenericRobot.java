/***
 * @author Adrian Schoenig, UNSW CSE, adrian.schoenig@gmail.com
 * @since 2008/07
 *
 * This JADE agent is modelling a robot which is implemented using the component-based
 * Orca framework. It requires at least a GoalEvaluator component to run in simulation
 * mode. For running on a robot, GoalPlanner and Localiser components are required as well.
 *
 * Behaviours of this agent
 * - TickerBehaviour which checks the DF every minute to update it's list of nearby robots
 * - TickerBehaviour which keeps track of own tasks and actions
 * - CyclicBehaviour which listens for requests for adding new tasks
 * - ContractNetResponder which listens for CFP to try to buy new goals
 * - ContractNetInitiator triggered by TickerBehaviour to occasionally sell it's own tasks
 *   executing new actions in order to achieve my goals/tasks and check whether I have
 *   recently achieved one of my goals/tasks
 */

import java.util.*;
import java.util.regex.*;
import java.io.*;

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
	enum WINNER_DETERMINATION { MIN_COST, REGRET_CLEARING };
	
	private final String CLEAR_MESSAGE = "!clear!";

	// Config values
	/////////////////////////////////////////////////////////////////////////////
	
	private boolean	 FULL_SIMULATION_MODE;
	private int	 		 CYCLE_TIME_RANDOM_TASK_GENERATION;
	private int	 		 CYCLE_TIME_ROBOT_DISCOVERY;
	private int	 		 CYCLE_TIME_ROBOT;
	private int	 		 CYCLE_TIME_TRIGGER_SALES;
	private int	 		 RANDOMISE_CYCLES_BY;
	
	private WINNER_DETERMINATION WINNER_DETERMINATION_METHOD;
	
	private int			 MAX_WAIT_TIME_FOR_RESPONSES_MS;
	private int			 MAX_WAIT_CYCLE_FOR_AUCTION_START;
	private int			 MAX_BUNDLES;
	private int			 BUNDLE_SIZE;
	
	private double	 CLOSE_ENOUGH_EPSILON_M;
	private long		 GOAL_EVAL_TIMEOUT_MS;
	private int			 GOAL_EVAL_MAX_TRY_COUNT;
	private long		 LOCALISER_TIMEOUT_MS;
	private String[] DEFAULT_ARGS = {""};

	private String 	 ORCA_NAME_GOAL_EVAL;
	private String 	 ORCA_NAME_PATH_FOLLOWER;
	private String 	 ORCA_NAME_LOCALISER;
	
	private String   GOAL_COORDS;

	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	private ArrayList<AID> 			buyer_robots		= new ArrayList<AID>();
	private ArrayList<Task2d>		tasks_for_sale	= new ArrayList<Task2d>();
	private Bundle2d						bundle					= new Bundle2d();
	private DEBUG 							debug_level 		= DEBUG.INFO;
	private int									active_auction_counter	= 0;
	private orca.Frame2d				current_target	= null;
	private orca.Frame2d				location = null;

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
		
		if(FULL_SIMULATION_MODE)
			log(DEBUG.WARNING, "We are running in full simulation mode. No interaction with robot drivers!");
		
		// check command line parameters
		// 1: String 	robot colour
		// 2: DEBUG 	debug level
		// 3: Float		start x coordinate
		// 4: Float		start y coordinate
		Object[] args = getArguments();
		if (args == null || args.length < 2) {
			log(DEBUG.ERROR, "Necessary parameters missing.");
			log(DEBUG.ERROR, "Please supply parameters: <robot_colour> <debug_level> [<start x> <start y>]");
			log(DEBUG.ERROR, "For example: red info -1 0.2");
			doDelete();
			return;
		}
		
		// initialise values
		String robo_colour = (String) args[0];
		this.debug_level = DEBUG.valueOf(((String) args[1]).toUpperCase());
		
		if (args.length < 4)
			this.location = createFrame(-1, 0);
		else
			this.location = createFrame(Float.valueOf((String) args[2]), Float.valueOf((String) args[3]));
		log(DEBUG.INFO, "My default location is " + location.p.x + " " + location.p.y);
		
		if (!loadConfig(robo_colour)) {
			doDelete();
			return;
		}
		
		this.bundle.tasks = new Task2d[0];
		this.bundle.cost  = 0;
		
		try {
 			/**************************** ORCA PROXIES *****************************/
			log(DEBUG.NOTE, "Connecting to Ice.");
			initProxies(robo_colour);
			
 			/************************** ADDING BEHAVIOURS **************************/
			log(DEBUG.NOTE, "Adding behaviours.");
			
			// randomise cycles if configured
			int r1, r2;
			if (RANDOMISE_CYCLES_BY > 0) {
				Random r = new Random();
				r1 = r.nextInt(RANDOMISE_CYCLES_BY * 1000) - RANDOMISE_CYCLES_BY * 1000 / 2;
				r2 = r.nextInt(RANDOMISE_CYCLES_BY * 1000) - RANDOMISE_CYCLES_BY * 1000 / 2;
			} else {
				r1 = r2 = 0;
			}
			
			// TickerBehaviour which keeps track of own tasks and actions
			addBehaviour(new RobotBehaviour(this, CYCLE_TIME_ROBOT * 1000));

			// TickerBehaviour which checks the DF every 30 secs to update it's list of nearby agents
			addBehaviour(new UpdateNeighbourBehaviour(this, CYCLE_TIME_ROBOT_DISCOVERY * 1000));
			
			// CyclicBehaviour which listens for requests of for adding new goals
			addBehaviour(new TaskReceiverBehaviour(this));
			
			// CyclicBehaviour which listens for CFP to try to buy new goals
			addBehaviour(new TriggerBuyerBehaviour(this));

			// TickerBehaviour which occasionally tries to sell it's own tasks
			addBehaviour(new TriggerSaleBehaviour(this, CYCLE_TIME_TRIGGER_SALES * 1000 + r1));
			
			// TickerBehaviour that randomly adds some new tasks to my list
			if (CYCLE_TIME_RANDOM_TASK_GENERATION > 0) {
				addBehaviour(new RandomTaskGenerator(this, CYCLE_TIME_RANDOM_TASK_GENERATION * 1000 + r2));
			}
			
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
		
		String patheval_name = ORCA_NAME_GOAL_EVAL.replace("<robo_colour>", robo_colour);
		String localiser_name = ORCA_NAME_PATH_FOLLOWER.replace("<robo_colour>", robo_colour);
		String follower_name = ORCA_NAME_LOCALISER.replace("<robo_colour>", robo_colour);

		// Interface to PathEvaluator (same for all robots)
		try {
			Ice.ObjectPrx patheval_base = this.ic.stringToProxy(patheval_name);
			if (null == (this.patheval = PathEvaluatorPrxHelper.checkedCast(patheval_base)))
				throw new Error("Goal Evaluator proxy could not be based.");
				
		} catch (Exception e) {
			log(DEBUG.ERROR, "initProxies: could not connect to goal evaluator component (" + patheval_name + ").");
			e.printStackTrace();
		}
		
		if(!FULL_SIMULATION_MODE) {
			// Interface to Localiser (1 per robot)
			try {
				Ice.ObjectPrx localiser_base = this.ic.stringToProxy(localiser_name);
				if (null == (this.localiser = orca.Localise2dPrxHelper.checkedCast(localiser_base)))
					throw new Error("Localiser proxy could not be based.");

			} catch (Exception e) {
				log(DEBUG.WARNING, "initProxies: could not connect to " + robo_colour + " localiser component (" + localiser_name + ").");
				e.printStackTrace();
			}

			// Interface to GoalPlanner, i.e. follower (1 per robot)
			try {
				Ice.ObjectPrx follower_base = this.ic.stringToProxy(follower_name);
				if (null == (this.follower = orca.PathFollower2dPrxHelper.checkedCast(follower_base)))
					throw new Error("Goal Planner proxy could not be based.");

			} catch (Exception e) {
				log(DEBUG.WARNING, "initProxies: could not connect to " + robo_colour + " goal planner component (" + follower_name + ").");
				e.printStackTrace();
			}
		}			
	}

	/**
	 * Loads settings from .cfg files
	 * @param robo_colour Colour of the robot to load robot specific configuration
	 * @return true if config successfully loaded, false otherwise
	 */
	protected boolean loadConfig(String robo_colour) {
		String class_name = this.getClass().getName();

		// load default config
		Properties def = new Properties();
		try {
			FileInputStream default_fis = new FileInputStream(class_name + ".cfg");
			def.load(default_fis);
		} catch (Exception e) {
			log(DEBUG.ERROR, "Could not load default configuration file '" + class_name + ".cfg'. Terminating agent...");
			return false;
		}
		
		// load robot specific config
		Properties prop = new Properties(def);
		try {
			FileInputStream prop_fis = new FileInputStream(class_name + "." + robo_colour + ".cfg");
			prop.load(prop_fis);
		} catch (FileNotFoundException e) {
			log(DEBUG.WARNING, "Could not load robot specific configuration file '" + class_name + "." + robo_colour + ".cfg'. Using defaults only.");
		} catch (IOException e) {
			log(DEBUG.WARNING, "Error loading robot specific configuration file '" + class_name + "." + robo_colour + ".cfg'. Using defaults only.");
		}

		
		// set variables according to config files
		try {
			FULL_SIMULATION_MODE = Boolean.parseBoolean(prop.getProperty("robot.full_simulation_mode"));

			CYCLE_TIME_RANDOM_TASK_GENERATION = Integer.parseInt(prop.getProperty("robot.cycles.random_task_generation"));
			CYCLE_TIME_ROBOT_DISCOVERY = Integer.parseInt(prop.getProperty("robot.cycles.robot_discovery"));
			CYCLE_TIME_ROBOT = Integer.parseInt(prop.getProperty("robot.cycles.driving"));
			CYCLE_TIME_TRIGGER_SALES = Integer.parseInt(prop.getProperty("robot.cycles.trigger_sales"));
			
			RANDOMISE_CYCLES_BY = Integer.parseInt(prop.getProperty("robot.randomise_cycles_by"));

			WINNER_DETERMINATION_METHOD = WINNER_DETERMINATION.valueOf(prop.getProperty("auction.winner_determination.type").toUpperCase());
			MAX_WAIT_TIME_FOR_RESPONSES_MS = Integer.parseInt(prop.getProperty("auction.selling.timeout_for_responses"));
			MAX_WAIT_CYCLE_FOR_AUCTION_START = Integer.parseInt(prop.getProperty("auction.selling.max_wait_cycles_for_restart"));
			MAX_BUNDLES = Integer.parseInt(prop.getProperty("auction.bidding.max_bundles"));
			BUNDLE_SIZE = Integer.parseInt(prop.getProperty("auction.bidding.bundle_size"));

			CLOSE_ENOUGH_EPSILON_M = Double.parseDouble(prop.getProperty("robot.close_enough_eplison"));
			GOAL_EVAL_TIMEOUT_MS = Long.parseLong(prop.getProperty("robot.component.goal_eval.timeout"));
			GOAL_EVAL_MAX_TRY_COUNT = Integer.parseInt(prop.getProperty("robot.component.goal_eval.retries"));
			LOCALISER_TIMEOUT_MS = Long.parseLong(prop.getProperty("robot.component.localiser.timeout"));
			DEFAULT_ARGS[0] = prop.getProperty("robot.ice");

			GOAL_COORDS = prop.getProperty("world.config.goals");			

			ORCA_NAME_GOAL_EVAL = prop.getProperty("robot.component.goal_eval.name");			
			ORCA_NAME_PATH_FOLLOWER = prop.getProperty("robot.component.path_follower.name");			
			ORCA_NAME_LOCALISER = prop.getProperty("robot.component.localiser.name");			

		} catch (Exception e) {
			log(DEBUG.ERROR, "Configuration files could not be parsed. Please check with example file. Terminating agent...");
			e.printStackTrace();
			return false;
		}
		
		return true;
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

	/**
	 * Drives robot around to head target in committed bundle and updates
	 * committed bundle upon arrival at head target.
	 */
	protected void drive() {
		if (this.bundle.tasks != null && this.bundle.tasks.length > 0) {
			// check if I need to change my goal
			orca.Frame2d loc = getLocation();

			// update my bundle (and thus immediate goal) if necessary
			if (loc != null && this.current_target != null && closeEnough(loc, this.current_target)) {
				log(DEBUG.INFO, String.format("drive: I have arrived at my current target (x:%.1f, y:%.1f)", loc.p.x, loc.p.y));
				
				Bundle2d new_bundle = new Bundle2d();
				new_bundle.tasks = new Task2d[bundle.tasks.length - 1];
				for (int i = 0; i < new_bundle.tasks.length; i++) {
					new_bundle.tasks[i] = bundle.tasks[i+1];
				}

				Bundle2d[] bundles = getBundlesWithCosts(loc, new_bundle.tasks, new Task2d[0], 1, 1);
				if (bundles.length > 0)
					this.bundle = bundles[0];
				else
					log(DEBUG.WARNING, "drive: Could not determine new destination.");
			}
			
			// update my current target if necessary
			if (this.current_target == null || (this.bundle.tasks != null && this.bundle.tasks.length > 0 && !closeEnough(this.current_target, this.bundle.tasks[0].target))) {
				this.current_target = this.bundle.tasks[0].target;
				log(DEBUG.INFO, String.format("drive: Heading to new target at x:%.1f, y:%.1f", current_target.p.x, current_target.p.y));
				
				// drive to target
				orca.Waypoint2d waypoint = new orca.Waypoint2d();
				waypoint.target = current_target;
				
				// we are very generous
				// TODO: these should be set in a config file
				waypoint.timeTarget 					= new orca.Time(15, 0); // 5 minutes to reach target
				waypoint.distanceTolerance 		= (float) CLOSE_ENOUGH_EPSILON_M;  	// meters
			  waypoint.headingTolerance 		= (float) 360.0; 	// degree
			  waypoint.maxApproachSpeed   	= (float) 99.9; 	// m/sec
			  waypoint.maxApproachTurnrate	= (float) 180.0;	// deg/sec
				
				orca.Waypoint2d[] path = {waypoint};
				orca.PathFollower2dData target = new orca.PathFollower2dData();
				target.path = path;
				target.timeStamp = new orca.Time();
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
					if (result.id.equals(task.id) || now - start_time > GOAL_EVAL_TIMEOUT_MS)
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
			log(DEBUG.ERROR, String.format("getBundlesWithCosts: Goal evaluator did not return any results within %.1fs. Something must be wrong.", GOAL_EVAL_TIMEOUT_MS / 1000.0));
			return new Bundle2d[0];
		}
	}
	
	/**
	 * Retrieves location of robot. 
	 * @return Frame2d of location of robot (or constanat dummy value if in simulation mode)
	 */
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
					// just try again
					continue;
					
				} catch (Ice.NotRegisteredException e) {
					// localiser got deregistered
					log(DEBUG.WARNING, "getLocation: localiser no longer found through ice.");
					break;
					
				} catch (orca.DataNotExistException e) {
					log(DEBUG.WARNING, "getLocation: localiser returned DataNotExistException");
					break;
				}

				// we might be lucky
				if (data != null) {
					now = Calendar.getInstance().getTimeInMillis();
					if (now - start_time > LOCALISER_TIMEOUT_MS)
						keep_looking = false;
				}
			}

			if (data != null && data.hypotheses.length != 0) {
				// use value from localiser
				location = data.hypotheses[0].mean;
				log(DEBUG.NOTE, "getLocation: my location is x:" + location.p.x + " y:" + location.p.y);
			}	else {
				log(DEBUG.ERROR, "getLocation: localiser did not return valid hypothesis for my location.");
			}

		}
		
		if (FULL_SIMULATION_MODE && location == null) {
			// use default value for debugging if no data found (print warning in that case)
			location = this.location;
			
			if(!FULL_SIMULATION_MODE)
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
			log(DEBUG.WARNING, "setNewBundles: handling of inconsistent bundles has not been added yet, but this bundle is actually inconsistent.");
			
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
		
		HashMap< Integer, PriorityQueue<Bid> > task_bid_map = new HashMap< Integer, PriorityQueue<Bid> >();
		
		// iterate over all agents and collect bids
		Set< Map.Entry< ACLMessage, ArrayList<Bundle2d> > > set = bids.entrySet();
		for (Map.Entry< ACLMessage, ArrayList<Bundle2d> > me : set) {
			ACLMessage bidder = me.getKey();
			log(DEBUG.SPAM, "selectWinningBundles: checking bids from " + bidder.getSender().getName() + ", who has " + bids.get(bidder).size() + " bids.");
			
			// iterate over all bundles in this package
			for (Bundle2d bundle: me.getValue()) {
				// skip bundle if not verified
				ArrayList<Integer> task_indices = getTaskForSaleIndex(bundle);

				if (task_indices.size() == 0) {
					// TODO: punish agent
					log(DEBUG.WARNING, "selectWinningBundles: Received invalid bid from " + bidder.getSender().getName());
					continue; // skip this bid

				} else if (task_indices.size() > 1) {
					// TODO: handle bundle-bids
					log(DEBUG.ERROR, "selectWinningBundles: No support for bundle-bids implemented yet.");
					continue; // skip this bid
				}
				
				// put bid into queue of bids for the task this bid is for
				Integer task_index = task_indices.get(0);
				if (!task_bid_map.containsKey(task_index)) {
					task_bid_map.put(task_index, new PriorityQueue<Bid>());
				}
				
				task_bid_map.get(task_index).add(new Bid(bidder, bundle));
				
			}
		}
		
		// determine winning bundles
		HashMap<ACLMessage, Bundle2d> winners = new HashMap<ACLMessage, Bundle2d>();

		Bundle2d 		best_bundle = null;
		ACLMessage	best_offer = null;
		float  			best_cost;
		
		Set< Map.Entry< Integer, PriorityQueue<Bid> > > task_bid_set = task_bid_map.entrySet();

		switch (WINNER_DETERMINATION_METHOD) {
			case MIN_COST:
				// the cheapest bid wins
				best_cost = Float.POSITIVE_INFINITY;
				
				for (Map.Entry< Integer, PriorityQueue<Bid> > me : task_bid_set) {
					Integer i = me.getKey();
					Bid head = me.getValue().poll();
					
					if (head.bundle.cost < best_cost) {
						best_bundle = head.bundle;
						best_cost 	= head.bundle.cost;
						best_offer 	= head.bidder;
					}
				}
				break;
				
			case REGRET_CLEARING:
				// the bid we regret the least wins, i.e. the bid that maximises the difference of
				// the costs from the best and the second best bidders
				best_cost = 0;

				for (Map.Entry< Integer, PriorityQueue<Bid> > me : task_bid_set) {
					Integer i 	= me.getKey();
					Bid first 	= me.getValue().poll();
					Bid second 	= me.getValue().poll();
					
					log(DEBUG.SPAM, "selectWinningBundles: diff of this is " + (second.bundle.cost - first.bundle.cost));
					
					if (second == null || second.bundle.cost - first.bundle.cost > best_cost) {
						best_bundle = first.bundle;
						best_cost 	= second.bundle.cost - first.bundle.cost;
						best_offer 	= first.bidder;

						log(DEBUG.SPAM, "selectWinningBundles: new min of " + best_cost);
					}
				}
				break;
			
		}



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
		log(DEBUG.WARNING, "validateBundleConsistency: not yet implemented.");
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
		return Math.abs(a - b) < CLOSE_ENOUGH_EPSILON_M;
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
			log(DEBUG.SPAM, "RandomTaskGenerator::onTick()");
			
			Random 	r = new Random();
			
			int 		num = r.nextInt(5) + 2;
			// TODO: just temp
			// num = 10;
			// while (tasks_for_sale.size() < num) {
				
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
			
			// TODO. just temp
			// log(DEBUG.SPAM, "Message: " + createTaskString(tasks_for_sale));
			// tasks_for_sale.clear();
		}
	}
	
	// TickerBehaviour which keeps track of own tasks and actions
	private class RobotBehaviour extends TickerBehaviour {
		private int last_task_count;
		
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
			log(DEBUG.SPAM, "RobotBehaviour::onTick()");
			
			
			int current_tasks = 0;
			if (bundle.tasks != null && bundle.tasks.length > 0)
				current_tasks = bundle.tasks.length;
			
			if (current_tasks != last_task_count) {
				// some debugging blah blah
				if (current_tasks > 0)
					log(DEBUG.INFO, "RobotBehaviour: I am currently committed to " + bundle.tasks.length + " tasks costing " + bundle.cost + ", and have " + tasks_for_sale.size() + " tasks for sale.");
				else
					log(DEBUG.NOTE, "RobotBehaviour: I am currently committed to no tasks, and have " + tasks_for_sale.size() + " tasks for sale.");
			}
			
			// handle driver
			if(!FULL_SIMULATION_MODE)
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
			log(DEBUG.SPAM, "TaskBuyerBehaviour::handleAcceptProposal()");
			
			
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
				String content = msg.getContent();
				log(DEBUG.SPAM, "TaskReceiverBehaviour: Received a request message.");
				log(DEBUG.SPAM, "TaskReceiverBehaviour: Content is:\n" + content);
				
				// analyse message content
				if (content.equals(CLEAR_MESSAGE)) {
					// received a request to clear all tasks, e.g. for re-planning
					log(DEBUG.INFO, "TaskReceiverBehaviour: Received request to clear all my tasks.");
					
					// clear committed tasks and answer message with my current bundle
					ACLMessage response = msg.createReply();
					response.setPerformative(ACLMessage.INFORM);
					Bundle2d[] current_bundle = {bundle};
					response.setContent( createBundleString(current_bundle) );
					
					bundle = null;
					
				} else {
					// check if we just got a bunch of new tasks assigned
					ArrayList<Task2d> tasks = extractTasks(content);
					if (tasks.size() > 0) {
						log(DEBUG.INFO, "TaskReceiverBehaviour: Received " + tasks.size() + " new tasks.");
						for(Task2d t: tasks) {
							tasks_for_sale.add(t);
							log(DEBUG.SPAM, "x: " + t.target.p.x + " y: " + t.target.p.y);
						}
					}
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
			cfp = new ACLMessage(ACLMessage.CFP);
			String message = createTaskString(tasks_for_sale);
			cfp.setContent(message);
			cfp.setProtocol("fipa-contract-net");
			cfp.setReplyByDate(new Date(System.currentTimeMillis() + MAX_WAIT_TIME_FOR_RESPONSES_MS));
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
			/*
			if (tasks_for_sale.size() > 0 && buyer_robots.size() > 0) {
				log(DEBUG.SPAM, "TaskSellerBehaviour::handleAllResponses: re-starting TaskSaleBehaviour");
				active_auction_counter = MAX_WAIT_CYCLE_FOR_AUCTION_START;
				myAgent.addBehaviour(new TaskSellerBehaviour(tasks_for_sale));
			} else
			*/
				active_auction_counter = 0;
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
			if (--active_auction_counter < 0 && tasks_for_sale.size() > 0) {
				if (buyer_robots.size() == 0) {
					log(DEBUG.WARNING, "TriggerSaleBehaviour: there are no robots to sell my tasks to.");
				} else {
					log(DEBUG.SPAM, "TriggerSaleBehaviour: starting TaskSaleBehaviour");
					active_auction_counter = MAX_WAIT_CYCLE_FOR_AUCTION_START;
					myAgent.addBehaviour(new TaskSellerBehaviour(tasks_for_sale));
				}

			} else if (active_auction_counter >= 0 && tasks_for_sale.size() > 0) {
				log(DEBUG.SPAM, "TriggerSaleBehaviour: can't start new auction, as old auction still busy (counter: " + active_auction_counter + ")");

			} else {
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