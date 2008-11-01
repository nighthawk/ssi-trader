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

public class AuctionAgent extends Agent {

	// Constants
	/////////////////////////////////////////////////////////////////////////////
	enum DEBUG 									{ SPAM, NOTE, INFO, WARNING, ERROR };
	enum OBJECTIVE							{ MINISUM, MINIMAX };
	enum WINNER_DETERMINATION 	{ MIN_COST, REGRET_CLEARING, ALL };
	enum DYNAMIC_ALLOCATION 		{ SSI, REPLAN, RESELL, ALL };

	protected final String CLEAR_MESSAGE = "!clear!";
	
	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	protected String 								GOAL_COORDS;
	protected double								CLOSE_ENOUGH_EPSILON_M;
	protected int	 		 							CYCLE_TIME_ROBOT_DISCOVERY;
	protected int										CYCLE_TIME_TRIGGER_SALES;
	protected int										RANDOMISE_CYCLES_BY;
	protected int			 							MAX_WAIT_TIME_FOR_RESPONSES_MS;
	protected int			 							MAX_WAIT_CYCLE_FOR_AUCTION_START;
	protected WINNER_DETERMINATION 	WINNER_DETERMINATION_METHOD;
	protected String[]							AGENT_NAMES;

	protected DEBUG 								debug_level 		= DEBUG.ERROR;
	protected ArrayList<Task2d> 		all_possible_tasks = new ArrayList<Task2d>();;
	protected ArrayList<Task2d>			tasks_for_sale	= new ArrayList<Task2d>();
	protected ArrayList<AID> 				buyer_robots		= new ArrayList<AID>();
	protected int										active_auction_counter	= 0;
	protected Properties						properties			= new Properties();

	// Agent life cycle methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Called on start by JADE
	 */
	protected void setup() {
		log(DEBUG.SPAM, "AuctionAgent: Starting up.");
	}
	
	/**
	 * Pre-death clean up called by JADE
	 */
	protected void takeDown() {
		log(DEBUG.SPAM, "AuctionAgent says Goodbye!");
	}

	// Internal methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Loads settings from .cfg files
	 * @param robo_colour Colour of the robot to load robot specific configuration
	 * @return true if config successfully loaded, false otherwise
	 */
	protected boolean loadConfig(String class_name) {
		if (loadConfig(class_name, true) != null) {
			return true;
		} else {
			log(DEBUG.ERROR, "Could not load default configuration file '" + class_name + ".cfg'. Terminating agent...");
			return false;
		}
	}
	protected Properties loadConfig(String class_name, boolean into_defaults) {
		log(DEBUG.SPAM, "loadConfig(): loading file '" + class_name + ".cfg'.");
		
		// load config
		Properties def;
		if (into_defaults)
			def = new Properties(this.properties);
		else
			def = new Properties();


		try {
			FileInputStream default_fis = new FileInputStream(class_name + ".cfg");
			def.load(default_fis);
			default_fis.close();
		} catch (Exception e) {
			return null;
		}
		
		if (into_defaults)
			this.properties = def;

		return def;
	}	
	
	protected boolean loadConfigValues() {
		// set variables according to config files
		try {
			CYCLE_TIME_ROBOT_DISCOVERY = Integer.parseInt(properties.getProperty("robot.cycles.robot_discovery"));
			CYCLE_TIME_TRIGGER_SALES = Integer.parseInt(properties.getProperty("robot.cycles.trigger_sales"));
			
			RANDOMISE_CYCLES_BY = Integer.parseInt(properties.getProperty("robot.randomise_cycles_by"));

			WINNER_DETERMINATION_METHOD = WINNER_DETERMINATION.valueOf(properties.getProperty("auction.winner_determination.type").toUpperCase());
			MAX_WAIT_TIME_FOR_RESPONSES_MS = Integer.parseInt(properties.getProperty("auction.selling.timeout_for_responses"));
			MAX_WAIT_CYCLE_FOR_AUCTION_START = Integer.parseInt(properties.getProperty("auction.selling.max_wait_cycles_for_restart"));

			CLOSE_ENOUGH_EPSILON_M = Double.parseDouble(properties.getProperty("world.config.close_enough"));
			GOAL_COORDS = properties.getProperty("world.config.goals");
			
			AGENT_NAMES = properties.getProperty("world.agents").replace("\"", "").split(" ");

		} catch (Exception e) {
			log(DEBUG.ERROR, "Configuration files could not be parsed. Please check with example file. Terminating agent...");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	protected void saveConfig(Properties prop, String file_name) {
		try {
			FileOutputStream output = new FileOutputStream(file_name + ".cfg");
			prop.store(output, null);
			output.close();
		} catch (Exception e) {
			log(DEBUG.ERROR, "Could not save configuration file '" + file_name + ".cfg'.");
		}
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
			System.out.println(String.format("%8s %8s - %s", getAID().getLocalName(), level.toString().toLowerCase(), message));
		}
	}

	protected Task2d[] getRandomTasks(int number, boolean allow_duplicates) {
		Task2d[] list = new Task2d[number];
		
		if (all_possible_tasks.size() == 0) {
			all_possible_tasks = extractTasks(GOAL_COORDS);
			log(DEBUG.SPAM, "getRandomTasks(): loaded " + all_possible_tasks.size() + " possible tasks.");
		}

		Random 	r = new Random();			
		int count = 0;

		while (count < number) {
			int 		k = r.nextInt(all_possible_tasks.size());
			Task2d 	t = all_possible_tasks.get(k);

			if (allow_duplicates || getTaskIndex(t, list) == -1) {
				log(DEBUG.NOTE, "getRandomFrames(): drawn random task (#" + k + ") x:" + t.target.p.x + " y:" + t.target.p.y);
				list[count++] = t;
			} else
				log(DEBUG.SPAM, "getRandomFrames(): not adding duplicate task (#" + k + ") x:" + t.target.p.x + " y:" + t.target.p.y);				
		}
		
		return list;
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
			log(DEBUG.SPAM, "selectWinningBundles: checking bids from " + bidder.getSender().getLocalName() + ", who has " + bids.get(bidder).size() + " bids.");
			
			// iterate over all bundles in this package
			for (Bundle2d bundle: me.getValue()) {
				// skip bundle if not verified
				ArrayList<Integer> task_indices = getTaskForSaleIndex(bundle);

				if (task_indices.size() == 0) {
					// TODO: punish agent
					log(DEBUG.WARNING, "selectWinningBundles: Received invalid bid from " + bidder.getSender().getLocalName());
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
				best_cost = Float.NEGATIVE_INFINITY;

				for (Map.Entry< Integer, PriorityQueue<Bid> > me : task_bid_set) {
					Integer i 	= me.getKey();
					Bid first 	= me.getValue().poll();
					Bid second 	= me.getValue().poll();
					
					float diff;
					if (second != null) {
						diff = second.bundle.cost - first.bundle.cost;

						log(DEBUG.SPAM, String.format("selectWinningBundles: diff of #%d for bidders %s:%.1f vs. %s:%.1f is %.1f.", i.intValue(), first.bidder.getSender().getLocalName(), first.bundle.cost, second.bidder.getSender().getLocalName(), second.bundle.cost, diff));
					} else
						diff = Float.POSITIVE_INFINITY;
					
					
					if (second == null || diff > best_cost) {
						best_bundle = first.bundle;
						best_cost 	= diff;
						best_offer 	= first.bidder;

						log(DEBUG.SPAM, "selectWinningBundles: new minimum regret of " + best_cost);
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
				log(DEBUG.SPAM, String.format("selectWinningBundles: task #%d to %s - %d tasks still for sale.", i, best_offer.getSender().getLocalName(), tasks_for_sale.size()));
			}

		}
		
		return winners;
	}
	

	// Helper methods
	/////////////////////////////////////////////////////////////////////////////

	protected <T> ArrayList<T> arrayToArrayList(T[] array) {
		ArrayList<T> al = new ArrayList<T>();
		for(T elem: array) {
			al.add(elem);
		}
		return al;
	}
	
	protected boolean closeEnough(double a, double b) {
		return Math.abs(a - b) < CLOSE_ENOUGH_EPSILON_M;
	}
	protected boolean closeEnough(orca.Frame2d a, orca.Frame2d b) {
		return (closeEnough(a.p.x, b.p.x) && closeEnough(a.p.y, b.p.y));
		
	}
	protected boolean closeEnough(Task2d a, Task2d b) {
		if (a == null || a == null)
			return false;
		else
			return closeEnough(a.target, b.target);
	}

	/**
	 * Helper function to create an orca.Frame2d object
	 * @param x coordinate
	 * @param y coordinate
	 * @return orca.Frame2d object
	 */
	protected orca.Frame2d createFrame(float x, float y) {
		return new orca.Frame2d(new orca.CartesianPoint2d(x, y), 0.0);
	}
	
	/**
	 * Helper function to create a talker.Task2d object
	 * @param x coordinate
	 * @param y coordinate
	 * @return talker.Task2d object
	 */
	protected Task2d createTask(float x, float y) {
		return new Task2d(createFrame(x, y));
	}

	/**
	 * Creates a message like "[0 1] [10.3 7]" from a list of Task2ds
	 * @param tasks ArrayList of Task2ds
	 * @param String of format "[<x> <y>] [<x> <y>] ..."
	 */
	protected String createTaskString(ArrayList<Task2d> tasks) {
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
	protected String createBundleString(Bundle2d[] bundles) {
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
	protected ArrayList<Task2d> extractTasks(String message) {
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
	protected ArrayList<Bundle2d> extractBundles(String message) {
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
	protected List<MatchResult> findAll(Pattern pattern, String text) {
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
	protected int getTaskIndex(Task2d needle, Task2d[] haystack) {
		if (haystack == null)
			return -1;
		
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
	
	// ContractNetInitiator, which sells tasks 
	protected class TaskSellerBehaviour extends ContractNetInitiator {
		public TaskSellerBehaviour(Agent a) {
			super(a, null);
		}
		
		/**
		 * Prepare call for proposal will be send to all agents
		 * @param cfp Empty ACLMessage which will be filled
		 * @return Vector of CFPs, though we'll only need one
		 */
		protected Vector<ACLMessage> prepareCfps(ACLMessage cfp) {
			active_auction_counter = MAX_WAIT_CYCLE_FOR_AUCTION_START;

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
			
			if (bids.size() != buyer_robots.size())
				log(DEBUG.WARNING, String.format("TaskSellerBehaviour::handleAllResponses: Only received bids from %d out of %d agents", bids.size(), buyer_robots.size()));
			
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

				log(DEBUG.NOTE, String.format("TaskSellerBehaviour::handleAllResponses: Selling the following bundle to " + offer.getSender().getLocalName() + ":\n" + bundle_string));
			}
		}
		
		protected void handleInform(ACLMessage inform) {
			if (tasks_for_sale.size() > 0 && buyer_robots.size() > 0) {
				log(DEBUG.SPAM, String.format("TaskSellerBehaviour::handleInform: re-starting TaskSaleBehaviour (%d tasks left).", tasks_for_sale.size()));
				active_auction_counter = MAX_WAIT_CYCLE_FOR_AUCTION_START;
				reset();
			} else {
				active_auction_counter = 0;
				myAgent.doWake();
			}
		}
		
	}
	
	// TickerBehaviour which occasionally tries to start the TaskSellerBehaviour
	protected class TriggerSaleBehaviour extends TickerBehaviour {
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
					myAgent.addBehaviour(new TaskSellerBehaviour(myAgent));
				}

			} else if (active_auction_counter >= 0 && tasks_for_sale.size() > 0) {
				log(DEBUG.SPAM, "TriggerSaleBehaviour: can't start new auction, as old auction still busy (counter: " + active_auction_counter + ")");

			} else {
				active_auction_counter = 0;
			}
		}
	}

	// TickerBehaviour which checks the DF to update it's list of nearby agents
	protected class UpdateNeighbourBehaviour extends TickerBehaviour {
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
					log(DEBUG.NOTE, "UpdateNeighbourBehaviour: Lost contact to " + Math.abs(diff) + " agent(s).");
				else if (diff > 0)
					log(DEBUG.NOTE, "UpdateNeighbourBehaviour: Got contact to  " + diff + " new agent(s).");

			} catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}
}