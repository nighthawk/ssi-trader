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
	private ArrayList<AgentController> 	controlled_agents = new ArrayList<AgentController>();
	private String 											config_filename;
	protected Properties								pos_properties			= new Properties();
	

	// Agent life cycle methods
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Called on start by JADE
	 */
	protected void setup() {
		super.setup();

		log(DEBUG.NOTE, "Experiment starting up.");
		
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
		
		int i = 0;
		int n_of_runs 		= Integer.parseInt((String) args[i++]);
		int n_of_robots 	= Integer.parseInt((String) args[i++]);
		int n_of_tasks 		= Integer.parseInt((String) args[i++]);
		// config_filename = String.format("%s-%d-%d-%d", this.getClass().getName(), n_of_runs, n_of_robots, n_of_tasks);
		config_filename = "ExperimentPositions";

		loadConfig("GenericRobot");
		pos_properties = loadConfig(config_filename, false);
		loadConfigValues();

		// setup instance variables
		ExperimentContainer experiment = new ExperimentContainer();
		experiment.number_of_runs		 					= n_of_runs;
		experiment.number_of_robots 					= n_of_robots;
		experiment.number_of_tasks 						= n_of_tasks;
		experiment.number_of_tasks_per_round	= Integer.parseInt((String) args[i++]);		
		this.debug_level 											= DEBUG.valueOf(((String) args[i++]).toUpperCase());
		
		if (args.length > 5) {
			experiment.winner_det	= WINNER_DETERMINATION.valueOf(((String) args[i++]).toUpperCase());
			experiment.dyn_alloc	= DYNAMIC_ALLOCATION.valueOf(((String) args[i++]).toUpperCase());
		}
		
		
		
		try {
 			/************************** ADDING BEHAVIOURS **************************/
			log(DEBUG.NOTE, "Adding behaviours.");
			
			addBehaviour(new ExperimentManager(this, experiment));

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

	private void sendClearMessageToNeighbours() {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setContent(CLEAR_MESSAGE);
		for (AID agent: buyer_robots)
			request.addReceiver(agent);
		send(request);
	}

	//===========================================================================
	// Inner class definitions
	//===========================================================================
	
	/**
	 * Container class which holds information about the current experiment
	 */
	private class ExperimentContainer {
		public ArrayList<Setting> settings;
		public ArrayList<Run> runs;		// contains all runs
		public boolean done;					// indicates if experiment is done

		public int number_of_runs;
		public int number_of_robots;
		public int number_of_tasks;
		public int number_of_tasks_per_round;

		public WINNER_DETERMINATION 	winner_det 			= WINNER_DETERMINATION.ALL;
		public DYNAMIC_ALLOCATION 		dyn_alloc				= DYNAMIC_ALLOCATION.ALL;
		
		/**
		 * Constructor
		 */
		public ExperimentContainer() {
			this.runs = new ArrayList<Run>();
			this.settings = new ArrayList<Setting>();
		}
		
		/**
		 * Get average runtime over all runs for a specified setting
		 * @param setting Setting for which the average runtime should be computed
		 * @return Average runtime for specified setting
		 */
		public double getAverageRuntime(Setting setting) {
			double sum = 0;
			for(Run run: runs)
				sum += run.results.get(setting).runtime;
			
			return sum / runs.size();
		}
		
		/**
		 * Get average total cost over all runs for a specified setting
		 * @param setting Setting for which the average total cost should be computed
		 * @return Average total cost for specified setting
		 */
		public double getAverageMaxCost(Setting setting) {
			double sum = 0;
			for (Run run: runs)
				sum += run.results.get(setting).max_cost;
			return sum / runs.size();
		}

		public double getAverageTotalCost(Setting setting) {
			double sum = 0;
			for (Run run: runs)
				sum += run.results.get(setting).total_cost;
			return sum / runs.size();
		}
		
		public void initialise() {
			log(DEBUG.INFO, String.format("New experiment with %d runs, %d robots, %d tasks (%d per round), %s and %s.", number_of_runs, number_of_robots, number_of_tasks, number_of_tasks_per_round, winner_det, dyn_alloc));
			
			// settings have to be initialised first
			initialiseSettings();			

			// set variables according to config files
			String tasks, robots;
			ArrayList<Task2d> task_list;

			log(DEBUG.SPAM, "initalise(): Trying to read task and robot positions...");
			for (int i = 1; i <= number_of_runs; i++) {
				Run r = new Run(this);

				// try to load experiment data
				try {
					task_list = extractTasks(pos_properties.getProperty("experiment.run." + i + ".robots"));
					log(DEBUG.NOTE, "initialise(): loaded " + task_list.size() + " robots.");
					if (task_list.size() < number_of_robots)
						throw new Exception("Regenerate please.");
					
					r.robots = new Task2d[number_of_robots];
					for (int k = 0; k < number_of_robots; k++)
						r.robots[k] = task_list.get(k);

				} catch (Exception e) {
					// draw random tasks
					e.printStackTrace();
					log(DEBUG.NOTE, "initalise(): creating random robots for run #" + i);
					r.drawRobots();
					pos_properties.put("experiment.run." + i + ".robots", createTaskString(arrayToArrayList(r.robots)));
				}

				// try to load experiment data
				try {
					task_list = extractTasks(pos_properties.getProperty("experiment.run." + i + ".tasks"));
					log(DEBUG.NOTE, "initialise(): loaded " + task_list.size() + " tasks.");
					if (task_list.size() < number_of_tasks)
						throw new Exception("Regenerate please.");

					r.tasks = new Task2d[number_of_tasks];
					for (int k = 0; k < number_of_tasks; k++)
						r.tasks[k] = task_list.get(k);

				} catch (Exception e) {
					// draw random tasks
					log(DEBUG.NOTE, "initalise(): creating random tasks for run #" + i);
					r.drawTasks();
					pos_properties.put("experiment.run." + i + ".tasks", createTaskString(arrayToArrayList(r.tasks)));
				}

				this.runs.add(r);
			}

			saveConfig(pos_properties, config_filename);
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
		
		/**
		 * Prints results (average run time and total cost) for the specified setting
		 * @param setting Setting for which the results should be printed
		 */
		public void printResults(Setting setting) {
			log(DEBUG.INFO, String.format("%18s %10s %10.2f %8.2f %8.2f", setting.winner_det, setting.dyn_alloc, getAverageRuntime(setting), getAverageTotalCost(setting), getAverageMaxCost(setting)));
		}

		public void printResultsHeader() {
			log(DEBUG.INFO, String.format("%18s %10s %10s %8s %8s", "win.det.", "dyn.all.", "run time", "sum", "max"));
		}
	}
	
	/**
	 * Container class which stores all information related to a particular run
	 */
	private class Run {
		public Task2d[] robots;											// starting points all robots
		public Task2d[] tasks;											// all tasks
		public HashMap< Setting, Result > results;	// stores result for each setting
		public boolean done;												// indicates if run is complete
		
		private int done_counter = 0;								// counter how many settings have been run
		private ExperimentContainer experiment;

		/**
		 * Constructor
		 * @param settings ArrayList of Settings for which this run is executed
		 */
		public Run(ExperimentContainer e) {
			this.experiment = e;
			
			results = new HashMap< Setting, Result >();
			for(Setting setting: experiment.settings)
				results.put(setting, new Result());
		}
		
		public void drawRobots() {
			log(DEBUG.SPAM, "Run::drawTasks(): drawing random robot positions.");
			robots = getRandomTasks( experiment.number_of_robots, true );
		}
		
		public void drawTasks() {
			log(DEBUG.SPAM, "Run::drawRobots(): drawing random task positions.");
			tasks  = getRandomTasks( experiment.number_of_tasks,  false );
		}
		
		/**
		 * Prints robot and task positions
		 */ 
		public void printPositions() {
			String robots = createTaskString(arrayToArrayList(this.robots));
			String tasks  = createTaskString(arrayToArrayList(this.tasks));
			
			log(DEBUG.INFO, "Results for run with the follow robot and task positions:\nRobots at " + robots + "\nTasks at " + tasks);
		}
		
		/**
		 * Prints results of all settings
		 */
		public void printResults() {
			for (Setting setting: results.keySet())
				printResults(setting);
		}

		/**
		 * Prints result of a single setting
		 * @param setting
		 */
		public void printResults(Setting setting) {
			Result result = results.get(setting);
			log(DEBUG.INFO, String.format("Setting: %s - %s, runtime: %.2f, total cost: %.2f, max cost: %.2f.", setting.winner_det, setting.dyn_alloc, result.runtime, result.total_cost, result.max_cost));
		}
		
		/**
		 * Sets status of a single setting to done (and for this run if all settings are done)
		 * @param setting
		 */
		public void setDone(Setting setting) {
			done_counter++;
			results.get(setting).done = true;
			
			log(DEBUG.SPAM, "Run::setDone(): " + done_counter + " settings of this run are done.");
			this.done = (done_counter == results.keySet().size());
		}
		
		/**
		 * Starts timer for a single setting
		 * @param setting
		 */
		public void timerStart(Setting setting) {
			results.get(setting).start_time = System.nanoTime() ;
		}
		
		/**
		 * Stops timer for a single setting and returns run time
		 * @param setting
		 * @return Runtime in ms (timerStart() has to be called before!)
		 */
		public double timerEnd(Setting setting) {
			Result r = results.get(setting);
			r.end_time = System.nanoTime();
			r.runtime = ((double) (r.end_time - r.start_time)) / 1000000.0;
			return r.runtime;
		}
	}
	
	/**
	 * Result including run time, total cost, timer and done indicator
	 */
	private class Result {
		public double runtime = Double.POSITIVE_INFINITY;
		public double total_cost = Double.POSITIVE_INFINITY;
		public double max_cost = Double.NEGATIVE_INFINITY;
		
		public long start_time;
		public long end_time;
		
		public boolean done;
	}

	/**
	 * Setting consisting of winner determination and dynamic allocation methods
	 */
	private class Setting {
		public WINNER_DETERMINATION winner_det;
		public DYNAMIC_ALLOCATION 	dyn_alloc;
		
		public Setting(WINNER_DETERMINATION win, DYNAMIC_ALLOCATION dyn) {
			this.winner_det = win;
			this.dyn_alloc  = dyn;
		}
	}

	private class ExperimentManager extends Behaviour {
		private ExperimentContainer experiment;
		private Iterator<Run> 		run_iterator = null;
		private Iterator<Setting> setting_iterator = null;
		private Run current_run = null;
		private Setting current_setting = null;
		private int counter = 0;

		public ExperimentManager(Agent a, ExperimentContainer e) {
			super(a);
			log(DEBUG.SPAM, "ExperimentManager::constructor()");
			
			this.experiment = e;
			this.experiment.initialise();
			
			this.run_iterator = experiment.runs.iterator();
		}
		


		public void action() {
			log(DEBUG.SPAM, "ExperimentManager::action()");
			
			if (current_run != null && current_run.done && !run_iterator.hasNext()) {
				// we are done: print results
				experiment.printResultsHeader();
				for (Setting setting: experiment.settings)
					experiment.printResults(setting);
				
				experiment.done = true;

			} else if (current_setting == null || current_run.results.get(current_setting).done) {
				
				if (current_setting == null || current_run.done) {
					log(DEBUG.NOTE, "ExperimentManager: Starting a new run.");
					current_run = run_iterator.next();
					setting_iterator = experiment.settings.iterator();
				}
				
				// advance setting
				current_setting = setting_iterator.next();
				log(DEBUG.NOTE, String.format("ExperimentManager: Run #%d with %s - %s:", experiment.runs.size(),  current_setting.winner_det, current_setting.dyn_alloc));
				
				// start experiment behaviours
				SequentialBehaviour expBeh = new SequentialBehaviour(this.myAgent);
				expBeh.addSubBehaviour(new RunStarter(this.myAgent, ++counter, current_run, current_setting));
				expBeh.addSubBehaviour(new BuyerCounter(this.myAgent, 1000, experiment.number_of_robots, current_run, current_setting));
				expBeh.addSubBehaviour(new RunManager(this.myAgent, current_run, current_setting));
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
		private int state;
		private int response_count;
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
						sendClearMessageToNeighbours();
						
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
							log(DEBUG.NOTE, String.format("RunAnalyser::action(): %s bought %d tasks for %.2f (%d/%d)", msg.getSender().getLocalName(), response.tasks.length, response.cost, this.response_count, this.run.experiment.number_of_robots));
							
							// update total cost
							this.run.results.get(this.setting).total_cost += response.cost;
							
							// update max cost
							if (response.cost > this.run.results.get(this.setting).max_cost)
								this.run.results.get(this.setting).max_cost = response.cost;
						}
						
						if (this.response_count == this.run.experiment.number_of_robots)
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
									log(DEBUG.SPAM, "RunAnalyser::action: Good bye, " + controller.getName());
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
	
	public class RunManager extends Behaviour {
		private Run run;
		private Setting setting;
		private ArrayList<Task2d> remaining_tasks;
		
		private int round_counter = 0;
		private int state = 0;
		private int response_count = 0;
		private  MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
		public RunManager(Agent a, Run r, Setting s) {
			super(a);
			this.run = r;
			this.setting = s;
			this.remaining_tasks = arrayToArrayList(r.tasks);
		}
		
		public void action() {
			
			// check if we need to start another round of selling
			if (tasks_for_sale.size() == 0 && active_auction_counter == 0) {
				
				// prepare new round
				switch (setting.dyn_alloc) {
					case SSI:
						// fall through: nothing needs to be done in preparation for the next round
						round_counter++;
						break;
					
					case REPLAN:
						// clear tasks of all robots and wait for answer
						if (round_counter == 0) {
							round_counter++;
							break;
						}
						
						switch (state) {
							case 0:
								log(DEBUG.SPAM, "RunManager::action(): New Round. Sending clear message to neighbours.");
								sendClearMessageToNeighbours();
								this.response_count = 0;
								state = 1;
								return;
							
							case 1:
								ACLMessage msg = myAgent.receive(mt);
								if (msg != null) {
									this.response_count++;
									log(DEBUG.SPAM, "RunManager::action(): Received response #" + this.response_count);
								}

								if (this.response_count == this.run.experiment.number_of_robots) {
									log(DEBUG.SPAM, String.format("RunManager::action(): Received responses from all %d agents.", this.run.experiment.number_of_robots));
									round_counter++;
									break;
								}
								else
									return;
						}
						
						
						break;
					
					case RESELL:
						// TODO: implement, i.e. tell other robots enter stage of reselling their tasks
						log(DEBUG.WARNING, "RunManager::action(): Not yet implemented for DYNAMIC_ALLOCATION.RESELL");
						round_counter++;
						break;
				}

				// resell tasks
				tasks_for_sale = nextTasks();
				if (tasks_for_sale.size() > 0) {
					log(DEBUG.SPAM, "RunManager: ################################");
					log(DEBUG.NOTE, String.format("RunManager: Starting round #%d of selling...", round_counter));
					myAgent.addBehaviour(new TaskSellerBehaviour(myAgent));
					this.state = 0;
				}
			} else {
				block();
			}
		}
		
		public boolean done() {
			return this.remaining_tasks.size() == 0 && tasks_for_sale.size() == 0;
		}
		
		private ArrayList<Task2d> nextTasks() {
			ArrayList<Task2d> tasks = new ArrayList<Task2d>();
			
			switch (setting.dyn_alloc) {
				case SSI:
					// just continue with the next ones
					while (tasks.size() < this.run.experiment.number_of_tasks_per_round && this.remaining_tasks.size() > 0)
						tasks.add(this.remaining_tasks.remove(0));

					break;
				
				case REPLAN:
					// start all over at each round again
					tasks = (ArrayList<Task2d>) this.remaining_tasks.clone();
					
					int limit = this.run.experiment.number_of_tasks_per_round * round_counter;
					log(DEBUG.SPAM, String.format("RunManager::nextTasks(): %d tasks/round, round #%d, limit: %d", this.run.experiment.number_of_tasks_per_round, round_counter, limit));
					if (limit >= this.remaining_tasks.size()) {
						// we are done after this round
						this.remaining_tasks.clear();
					} else {
						// remove tail
						while (tasks.size() > limit)
							tasks.remove(tasks.size()-1);
					}
					
					break;
					
				case RESELL:
					// TODO: implement
					log(DEBUG.WARNING, "RunManager::nextTasks(): Not yet implemented for DYNAMIC_ALLOCATION.RESELL");
					this.remaining_tasks.clear();
					break;
			}
			
			return tasks;
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
				Object[] args = { AGENT_NAMES[agent_count], properties.getProperty("robot.children.debug"), task.target.p.x + "", task.target.p.y + "" };
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
					log(DEBUG.NOTE, "BuyerCounter::onTick(): Got contact to  " + diff + " new agent(s).");

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
