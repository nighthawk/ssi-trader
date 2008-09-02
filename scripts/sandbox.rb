require "Ice"
require "talker/pathevaluator.rb"

def start

end

def create_task(x, y, t)
  task = Talker::Task2d.new
  task.target.p.x = x
  task.target.p.y = y
  task.target.o = t
  return task
end

def get_test_results
  options = !ARGV.empty? ? ARGV : ["--Ice.Default.Locator=IceGrid/Locator:default -p 12000"]

  puts "Connecting with: #{options}"

  ic = Ice::initialize(options)
  base = ic.stringToProxy("pathevaluator@vm-ubuntu/pathevaluator")

  # trying ping to see if remote proxy exists
  puts base.ice_ping

  p = Talker::PathEvaluatorPrx::checkedCast(base)
  puts p

  # create a task for the path evaluator
  task = Talker::PathEvaluatorTask.new

  # our current location
  start = Orca::Frame2d.new
  start.p.x = -1
  start.p.y = 0
  start.o		= 0

  tasks = []
  tasks << create_task( -9,  0, 0);
  tasks << create_task( -1,  3, 0);
  tasks << create_task( 23, -4, 0);
  tasks << create_task(-23,  8, 0);

  committed = []
  committed << create_task( 11,  0, 0);

  task.maxBundles 		= 15;
  task.bundleSize 		= 3;
  task.start 					= start;
  task.committedTasks = committed;
  task.newTasks 			= tasks;

  p.setTask(task)
  
  return p.getData
end

get_test_results

