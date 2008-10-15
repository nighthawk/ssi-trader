import java.util.*;

import jade.core.*;
import jade.core.behaviours.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;
import jade.wrapper.*;

import talker.*;

public class Experiment extends AuctionAgent {

	// Constants
	/////////////////////////////////////////////////////////////////////////////
    
	// Instance variables
	/////////////////////////////////////////////////////////////////////////////
	private int number_of_runs;
	private int number_of_robots;
	private int number_of_tasks;
	private int number_of_tasks_per_round;
	
	private WINNER_DETERMINATION 	winner_det 			= WINNER_DETERMINATION.ALL;
	private DYNAMIC_ALLOCATION 		dyn_alloc				= DYNAMIC_ALLOCATION.ALL;
	
	private ArrayList<AgentController> controlled_agents = new ArrayList<AgentController>();
	

	// Agent life cycle methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Called on start by JADE
	 */
	protected void setup() {
		super.setup();

		log(DEBUG.INFO, "Experiment starting up.");
		
		// check command line parameters
		// 1: Int			Number of runs
		// 2: Int 		Number of robots
		// 3: Int			Number of tasks
		// 5: Int			Number of tasks per round
		// 2: DEBUG 	Debug level
		// 2: WIN_DET Type of winner determination, or ALL
		// 2: DYN_ALC Type of dynamic allocation, or ALL
		Object[] args = getArguments();
		if (args == null || args.length < 5) {
			log(DEBUG.ERROR, "Necessary parameters missing.");
			log(DEBUG.ERROR, "Please supply parameters: <#runs> <#robot> <#tasks> <#tasks per round> <debug level> [<winner determination type> <dynamic allocation type>]");
			log(DEBUG.ERROR, "For example: 25 5 10 2 INFO ALL SSI");
			doDelete();
			return;
		}

		// setup instance variables
		int i = 0;
		this.number_of_runs		 					= Integer.parseInt((String) args[i++]);
		this.number_of_robots 					= Integer.parseInt((String) args[i++]);
		this.number_of_tasks 						= Integer.parseInt((String) args[i++]);
		this.number_of_tasks_per_round	= Integer.parseInt((String) args[i++]);		
		this.debug_level 								= DEBUG.valueOf(((String) args[i++]).toUpperCase());
		
		if (args.length > 5) {
			this.winner_det								= WINNER_DETERMINATION.valueOf(((String) args[i++]).toUpperCase());
			this.dyn_alloc							 	= DYNAMIC_ALLOCATION.valueOf(((String) args[i++]).toUpperCase());
		}
		
		loadConfig("GenericRobot");
		loadConfigValues();
		
		
		try {
 			/************************** ADDING BEHAVIOURS **************************/
			log(DEBUG.NOTE, "Adding behaviours.");
			
			addBehaviour(new ExperimentManager(this));

		} catch (Exception e) { 
			e.printStackTrace();
		}
	}
	
	/**
	 * Pre-death clean up called by JADE
	 */
	protected void takeDown() {
		super.takeDown();
	}

	// Internal methods
	/////////////////////////////////////////////////////////////////////////////


	//===========================================================================
	// Inner class definitions
	//===========================================================================
	
	private class ExperimentContainer {
		public ArrayList<Run> runs;
		public boolean done;
		
		public ExperimentContainer() {
			runs = new ArrayList<Run>();
		}
		
		public double getAverageRuntime(Setting setting) {
			double sum = 0;
			for(Run run: runs)
				sum += run.results.get(setting).runtime;
			
			return sum / runs.size();
		}
		
		public double getAverageCost(Setting setting) {
			double sum = 0;
			for (Run run: runs)
				sum += run.results.get(setting).total_cost;
			return sum / runs.size();
		}
		
		public void printResults(Setting setting) {
			log(DEBUG.INFO, "Overall results for " + setting.winner_det + " - " + setting.dyn_alloc + ":");
			log(DEBUG.INFO, "Runtime: " + getAverageRuntime(setting) + ", cost: " + getAverageCost(setting));
		}
	}
	
	private class Run {
		public Task2d[] robots;
		public Task2d[] tasks;		
		public HashMap< Setting, Result > results;
		public boolean done;
		
		private int done_counter = 0;

		public Run(ArrayList<Setting> settings) {
			log(DEBUG.SPAM, "Run::constructor(): drawing random robot positions.");
			robots = getRandomTasks( number_of_robots, true );
			
			log(DEBUG.SPAM, "Run::constructor(): drawing random task positions.");
			tasks  = getRandomTasks( number_of_tasks,  false );
			
			results = new HashMap< Setting, Result >();
			for(Setting setting: settings)
				results.put(setting, new Result());
		}
		
		public void printPositions() {
			String robots = createTaskString(arrayToArrayList(this.robots));
			String tasks  = createTaskString(arrayToArrayList(this.tasks));
			
			log(DEBUG.INFO, "Results for run with the follow robot and task positions:\nRobots at " + robots + "\nTasks at " + tasks);
		}
		
		public void printResults() {
			for (Setting setting: results.keySet())
				printResults(setting);
		}

		public void printResults(Setting setting) {
			Result result = results.get(setting);
			log(DEBUG.INFO, "Setting: " + setting.winner_det + " - " + setting.dyn_alloc + " has run time " + result.runtime + " and cost " + result.total_cost);
		}
		
		public void setDone(Setting setting) {
			done_counter++;
			results.get(setting).done = true;
			
			log(DEBUG.SPAM, "Run::setDone(): " + done_counter + " settings of this run are done.");
			this.done = (done_counter == results.keySet().size());
		}
		
		public void timerStart(Setting setting) {
			results.get(setting).start_time = Calendar.getInstance().getTimeInMillis();
		}
		
		public double timerEnd(Setting setting) {
			Result r = results.get(setting);
			r.end_time = Calendar.getInstance().getTimeInMillis();
			r.runtime = r.end_time - r.start_time;
			return r.runtime;
		}
	}
	
	private class Result {
		public double runtime = Double.POSITIVE_INFINITY;
		public double total_cost = Double.POSITIVE_INFINITY ;
		
		public double start_time;
		public double end_time;
		
		public boolean done;
	}

	private class Setting {
		public WINNER_DETERMINATION winner_det;
		public DYNAMIC_ALLOCATION 	dyn_alloc;
		
		public Setting(WINNER_DETERMINATION win, DYNAMIC_ALLOCATION dyn) {
			this.winner_det = win;
			this.dyn_alloc  = dyn;
		}
	}


	private class ExperimentManager extends Behaviour {
		private ArrayList<Setting> settings;

		private ExperimentContainer experiment;
		private Run current_run = null;
		private Iterator<Setting> iter_setting = null;
		private Setting current_setting = null;
		private int counter = 0;

		public ExperimentManager(Agent a) {
			super(a);
			log(DEBUG.SPAM, "ExperimentManager::constructor()");

			log(DEBUG.INFO, "ExperimentManager: Starting new experiment with the following settings:");
			log(DEBUG.INFO, "Number of runs: " + number_of_runs);
			log(DEBUG.INFO, "Number of robots: " + number_of_robots);
			log(DEBUG.INFO, "Number of tasks: " + number_of_tasks);
			log(DEBUG.INFO, "Number of tasks assigned per round: " + number_of_tasks_per_round);
			log(DEBUG.INFO, "Each run is tested with winner determination method: " + winner_det);
			log(DEBUG.INFO, "Each run is tested with dynamic allocation method: " + dyn_alloc);
			
			this.settings = new ArrayList<Setting>();
			initialiseSettings();
			
			experiment = new ExperimentContainer();
		}
		
		private void initialiseSettings() {
			log(DEBUG.SPAM, "ExperimentManager::initialiseSettings()");
			
			// determine what we have to iterate over
			ArrayList<DYNAMIC_ALLOCATION> alloc_types = new ArrayList<DYNAMIC_ALLOCATION>();
			ArrayList<WINNER_DETERMINATION> win_types = new ArrayList<WINNER_DETERMINATION>();

			// for each dynamic allocation type
			if (dyn_alloc == DYNAMIC_ALLOCATION.ALL) {
				for (DYNAMIC_ALLOCATION dyn: DYNAMIC_ALLOCATION.values())
					if (dyn != DYNAMIC_ALLOCATION.ALL)
						alloc_types.add(dyn);
			} else
				alloc_types.add(dyn_alloc);

			// for each winner determination type
			if (winner_det == WINNER_DETERMINATION.ALL) {
				for (WINNER_DETERMINATION win: WINNER_DETERMINATION.values())
					if (win != WINNER_DETERMINATION.ALL)
						win_types.add(win);
			} else
				win_types.add(winner_det);
			
			// create a setting for each combination of DYN_ALLOC & WIN_DET
			for (DYNAMIC_ALLOCATION dyn: alloc_types)
				for (WINNER_DETERMINATION win: win_types)
					this.settings.add( new Setting(win, dyn) );
		}

		public void action() {
			log(DEBUG.SPAM, "ExperimentManager::action()");
			
			if (current_run != null && current_run.done
					&& experiment.runs.size() == number_of_runs) {
				log(DEBUG.INFO, "ExperimentManager: Finished all " + experiment.runs.size() + " runs.");
				for (Setting setting: settings)
					experiment.printResults(setting);
				
				experiment.done = true;

			} else if (experiment.runs.size() <= number_of_runs && (current_run == null || current_setting == null || current_run.results.get(current_setting).done)) {
				
				if (current_run == null || current_run.done) {
					log(DEBUG.INFO, "ExperimentManager: Starting a new run.");
					// start a new run
					current_run = new Run(settings);
					experiment.runs.add(current_run);
					
					iter_setting = settings.iterator();
				}
				
				// advance setting
				current_setting = iter_setting.next();
				log(DEBUG.INFO, String.format("ExperimentManager: Run #%d with %s - %s:", experiment.runs.size(),  current_setting.winner_det, current_setting.dyn_alloc));
				
				// start experiment behaviours
				SequentialBehaviour expBeh = new SequentialBehaviour(this.myAgent);
				expBeh.addSubBehaviour(new RunStarter(this.myAgent, ++counter, current_run, current_setting));
				expBeh.addSubBehaviour(new BuyerCounter(this.myAgent, 1000, number_of_robots, current_run, current_setting));
				expBeh.addSubBehaviour(new TaskSellerBehaviour( arrayToArrayList(current_run.tasks) ));
				expBeh.addSubBehaviour(new RunAnalyser(this.myAgent, current_run, current_setting));
				expBeh.addSubBehaviour(new BuyerCounter(this.myAgent, 1000, 0, current_run, current_setting));

				myAgent.addBehaviour(expBeh);
			}
			
			// wait for myself to be restarted from another methods
			block();
		}
		
		public boolean done() {
			return experiment.done;
		}

	}


	public class RunAnalyser extends Behaviour {
		private Run run;
		private Setting setting;
		int state;
		int response_count;		
		private  MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		public RunAnalyser(Agent a, Run r, Setting s) {
			super(a);
			this.run = r;
			this.setting = s;
			this.response_count = 0;
			this.state = 0;
		}
		
		public void action() {
			log(DEBUG.SPAM, "RunAnalyser::action()");
			
			if(tasks_for_sale.size() == 0) {
				switch (this.state) {
					case 0:
						// sale just ended
						log(DEBUG.SPAM, "RunAnalyser::action(): Auction just ended...");
						
						this.run.timerEnd(this.setting);
						this.run.results.get(this.setting).total_cost = 0;
						
						// ask all for results
						ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
						request.setContent(CLEAR_MESSAGE);
						for (AID agent: buyer_robots)
							request.addReceiver(agent);
						send(request);
						
						// proceed to next state
						this.state = 1;
						break;
						
					case 1:
						// check results until we got all
						log(DEBUG.SPAM, "RunAnalyser::action(): Waiting for results from agents");

						ACLMessage msg = myAgent.receive(mt);
						if (msg != null) {
							this.response_count++;
							Bundle2d response = extractBundles(msg.getContent()).get(0);
							log(DEBUG.NOTE, String.format("RunAnalyser::action(): %s bought %d tasks for %.2f (%d/%d)", msg.getSender().getLocalName(), response.tasks.length, response.cost, this.response_count, number_of_robots));
							this.run.results.get(this.setting).total_cost += response.cost;
						}
						
						if (this.response_count == number_of_robots)
							this.state = 2;
						break;
						
					case 2:
						// print results and leave
						log(DEBUG.SPAM, "RunAnalyser::action(): Cleaning up...");

						run.printResults(this.setting);

						// clear agents
						log(DEBUG.NOTE, "RunAnalyser::action: Sending 'good bye' to " + controlled_agents.size() + " agents.");
						for (AgentController controller: controlled_agents) {
							try {
								if (controller != null) {
									log(DEBUG.NOTE, "RunAnalyser::action: Good bye, " + controller.getName());
									controller.kill();
								}
							} catch (StaleProxyException e) {
								log(DEBUG.ERROR, "RunAnalyser::action: Could not kill agent.");
								e.printStackTrace();
							}
						}

						controlled_agents.clear();
						
						this.run.setDone(this.setting);
						this.state = 3;
						break;
				}
			}
			
			block(100);
		}
		
		public boolean done() {
			return this.state == 3;
		}
	}
	

	/**
	 * Behaviour that sets up a new experiment, i.e. start new agents
	 */
	private class RunStarter extends OneShotBehaviour {
		private Run run;
		private Setting setting;
		private int counter;
		
		public RunStarter(Agent a, int counter, Run r, Setting s) {
			super(a);

			log(DEBUG.SPAM, "RunStarter::constructor()");
			this.run = r;
			this.setting = s;
			this.counter = counter;
		}
		
		public void action() {
			// set up robots
			log(DEBUG.SPAM, "RunStarter::action()");
			
			jade.wrapper.AgentContainer container = getContainerController();
			int agent_count = 0;
			for (Task2d task: run.robots) {
				log(DEBUG.SPAM, "Starting agent " + agent_count + ": " + AGENT_NAMES[agent_count] + counter);

				AgentController ac;
				Object[] args = { AGENT_NAMES[agent_count], "error", task.target.p.x + "", task.target.p.y + "" };
				try {
					ac = container.createNewAgent(AGENT_NAMES[agent_count] + counter,
					 															"GenericRobot",
																				args);
					controlled_agents.add(ac);
					log(DEBUG.SPAM, "Created agent as " + ac.getName() + ". Starting him up...");
					ac.start();

				} catch (StaleProxyException e) {
					log(DEBUG.ERROR, "Setting::runExperiment(): Error starting new agent.");
					e.printStackTrace();
				}
				agent_count++;
			}
			
			// setup up environment
			tasks_for_sale = arrayToArrayList(run.tasks);
			WINNER_DETERMINATION_METHOD = setting.winner_det;
		}
	}
	
	private class BuyerCounter extends TickerBehaviour {
		private Run run;
		private Setting setting;
		private int count;

		public BuyerCounter(Agent a, long p, int c, Run r, Setting s) {
			super(a, p);
			log(DEBUG.SPAM, "BuyerCounter::constructor()");
			this.run = r;
			this.setting = s;
			this.count = c;
		}

		/**
		 * Updates list of nearby agents
		 */
		public void onTick() {
			log(DEBUG.SPAM, "BuyerCounter::onTick()");

			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("task-buying");
			template.addServices(sd);
			
			int prev = buyer_robots.size();

			try {
				DFAgentDescription[] result = DFService.search(this.myAgent, template);
				buyer_robots.clear();
				for (DFAgentDescription d: result)
					buyer_robots.add(d.getName());
					
				log(DEBUG.SPAM, "BuyerCounter::onTick(): I have " + result.length + " neighbours.");
				int diff = buyer_robots.size() - prev;
				if (diff > 0)
					log(DEBUG.INFO, "BuyerCounter::onTick(): Got contact to  " + diff + " new agent(s).");

			} catch(FIPAException e) {
				e.printStackTrace();

			} finally {

				log(DEBUG.SPAM, "BuyerCounter::onTick(): Waiting for " + controlled_agents.size() + " to start. Currently we found " + buyer_robots.size() + ".");
				if (count == buyer_robots.size()) {
					log(DEBUG.SPAM, "BuyerCounter::onTick(): Found all agents. Stopping looking for new ones.");
					
					if (count > 0)
						this.run.timerStart(this.setting);

					stop();
				}
			}
		}
	}

}
