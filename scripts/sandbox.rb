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

  @p = Talker::PathEvaluatorPrx::checkedCast(base)
  puts @p

  # create a task for the path evaluator
  task = Talker::PathEvaluatorTask.new

  # our current location
  start = Orca::Frame2d.new

  # THIS COMBINATION CAUSES DUPLICATES WITH HIGH PERMUTATE_LAST
  start.p.x = -7
  start.p.y = 7
  start.o		= 0

  tasks = []
  tasks << create_task( -6, -5, 0);

  committed = []
  committed << create_task( -7,  7, 0);
  committed << create_task( -5, -1, 0);
  committed << create_task( 11, -8, 0);

  task.maxBundles 		= 25;


  task.bundleSize 		= 1;
  task.start 					= start;
  task.committedTasks = committed;
  task.newTasks 			= tasks;
  task.sender         = "ruby_test"
  task.id             = "ruby_id"

  @p.setTask(task)
  
  continue = true
  while(continue)
    @data = @p.getData(task.sender)
    continue = (@data.id != task.id)
  end
  
  return @data
end

def print results
  results.data.each do |e|
    out = "#{e.cost}: "
    e.tasks.each { |t| out += " [#{t.target.p.x} #{t.target.p.y}]"  }
    puts out
  end
end

print get_test_results

