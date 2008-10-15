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

import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;
import jade.proto.*;

import talker.*;

public class GenericRobot extends AuctionAgent {
	// Constants
	/////////////////////////////////////////////////////////////////////////////

	// Config values
	/////////////////////////////////////////////////////////////////////////////
	
	private boolean	 FULL_SIMULATION_MODE;
	private int	 		 CYCLE_TIME_RANDOM_TASK_GENERATION;
	private int	 		 CYCLE_TIME_ROBOT;
	
	private int			 MAX_BUNDLES;
	private int			 BUNDLE_SIZE;
	
	private long		 GOAL_EVAL_TIMEOUT_MS;
	private int			 GOAL_EVAL_MAX_TRY_COUNT;
	private long		 LOCALISER_TIMEOUT_MS;
	private String[] DEFAULT_ARGS = {""};

	private String 	 ORCA_NAME_GOAL_EVAL;
	private String 	 ORCA_NAME_PATH_FOLLOWER;
	private String 	 ORCA_NAME_LOCALISER;

	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	private Bundle2d						bundle					= new Bundle2d();
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
		super.setup();
		
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
		
		String class_name = this.getClass().getName();
		if (!loadConfig(class_name) || !loadConfigValues()) {
			doDelete();
			return;
		}
		
		this.bundle.tasks = new Task2d[0];
		this.bundle.cost  = 0;
		
		try {
 			/**************************** ORCA PROXIES *****************************/
			log(DEBUG.NOTE, "Connecting to Ice.");
			if (!initProxies(robo_colour)) {
				doDelete();
				return;
			}
			
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
		super.takeDown();
		
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
	protected boolean initProxies(String robo_colour) {
		// connect to Ice
		log(DEBUG.SPAM, "initProxies: Ice is connecting to " + DEFAULT_ARGS[0]);
		this.ic = Ice.Util.initialize(DEFAULT_ARGS);
		
		String patheval_name 	= ORCA_NAME_GOAL_EVAL.replace("<robo_colour>", robo_colour);
		String localiser_name = ORCA_NAME_PATH_FOLLOWER.replace("<robo_colour>", robo_colour);
		String follower_name 	= ORCA_NAME_LOCALISER.replace("<robo_colour>", robo_colour);

		// Interface to PathEvaluator (same for all robots)
		try {
			Ice.ObjectPrx patheval_base = this.ic.stringToProxy(patheval_name);
			if (null == (this.patheval = PathEvaluatorPrxHelper.checkedCast(patheval_base)))
				throw new Error("Goal Evaluator proxy could not be based.");
				
		} catch (Exception e) {
			log(DEBUG.ERROR, "initProxies: could not connect to goal evaluator component (" + patheval_name + ").");
			e.printStackTrace();
			return false;
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
		
		return true;			
	}

	/**
	 * Loads settings from .cfg files
	 * @param robo_colour Colour of the robot to load robot specific configuration
	 * @return true if config successfully loaded, false otherwise
	 */
	protected boolean loadConfigValues() {
		super.loadConfigValues();
		
		// set variables according to config files
		try {
			FULL_SIMULATION_MODE = Boolean.parseBoolean(properties.getProperty("robot.full_simulation_mode"));

			CYCLE_TIME_RANDOM_TASK_GENERATION = Integer.parseInt(properties.getProperty("robot.cycles.random_task_generation"));
			CYCLE_TIME_ROBOT = Integer.parseInt(properties.getProperty("robot.cycles.driving"));

			MAX_BUNDLES = Integer.parseInt(properties.getProperty("auction.bidding.max_bundles"));
			BUNDLE_SIZE = Integer.parseInt(properties.getProperty("auction.bidding.bundle_size"));

			GOAL_EVAL_TIMEOUT_MS = Long.parseLong(properties.getProperty("robot.component.goal_eval.timeout"));
			GOAL_EVAL_MAX_TRY_COUNT = Integer.parseInt(properties.getProperty("robot.component.goal_eval.retries"));
			LOCALISER_TIMEOUT_MS = Long.parseLong(properties.getProperty("robot.component.localiser.timeout"));
			DEFAULT_ARGS[0] = properties.getProperty("robot.ice");
			
			ORCA_NAME_GOAL_EVAL = properties.getProperty("robot.component.goal_eval.name");			
			ORCA_NAME_PATH_FOLLOWER = properties.getProperty("robot.component.path_follower.name");			
			ORCA_NAME_LOCALISER = properties.getProperty("robot.component.localiser.name");			

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
				int wait_for = r.nextInt(1000);
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


	//===========================================================================
	// Inner class definitions
	//===========================================================================
	
	// TickerBehaviour that adds a random tasks
	private class RandomTaskGenerator extends TickerBehaviour {
		ArrayList<Task2d> all_possible_tasks;
		
		public RandomTaskGenerator(Agent a, long period) {
			super(a, period);
			
			onTick();
		}
		
		protected void onTick() {
			// add random tasks to my tasks for sale list
			Random 	r = new Random();			
			int 		num = r.nextInt(5) + 2;

			for (Task2d task: getRandomTasks(num, false)) {
				log(DEBUG.INFO, "RandomTaskGenerator: drawn random task x:" + task.target.p.x + " y:" + task.target.p.y);
				tasks_for_sale.add(task);
			}
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
			
			// sent confirmation back
			ACLMessage response = new ACLMessage(ACLMessage.INFORM);
			response.setProtocol("fipa-contract-net");
			return response;
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
					send(response);
					
					bundle = new Bundle2d();
					
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
	
}