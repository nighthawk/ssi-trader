require 'rubygems'
require 'rsruby'
require 'fastercsv'

class Object
  def interesting_methods
    self.methods.sort - Class.methods
  end
end

class Experiment
  attr_reader :data, :r
  
  def initialize(dir)
    # setup R
    ENV["R_HOME"] ||= "/Library/Frameworks/R.framework/Resources"
    @r = RSRuby.instance

    @data = {}
    @dir = dir || `pwd`.strip
  end
  
  def analyse(bigger, smaller, options = nil)
    # paired t-test
    options ||= {:paired => true, :alternative => 'greater'}
    res = @r.t_test bigger, smaller, options
    
    puts "#{res['method']} - testing for '#{res['alternative']}'"
    puts "Null hypothesis: #{res["null.value"]}."
    puts
    printf "p value: %.8f\n", res["p.value"]
    if res["p.value"] < 0.05
      printf "Null hypothesis rejected on 0.05 interval with confidence %.2f%\n", (1 - res["p.value"]) * 100
    else
      printf "Null hypothesis could not be rejected on 0.05 interval with confidence %.2f%\n", (1 - res["p.value"]) * 100
    end
    puts "Estimate: #{res['estimate']}"
    printf "Mean of difference is %.2f%\n", res["estimate"]["mean of the differences"] / @r.mean(bigger) * 100
    
    res
  end

  def boxplot(samples)
    names = []
    values = []
    samples.each { |s| names << s[:name]; values << s[:data] }
    p values
    p names
    
    @r.boxplot values, :names => names
  end
end

class CsvExperiment < Experiment
  def initialize(dir = nil)
    super(dir)
    
    load_files Dir.glob(@dir + '/**/*.csv')
  end
  
  def extract_sample(pot, row, &conditions)
    sample = []
    @data[pot].each do |d|
      unless d['#robots'].nil?
        if block_given?
          sample << d[row].to_f if yield d
        else
          sample << d[row].to_f
        end
      end
    end
    sample
  end
  
private
  def load_files(files)
    files.each do |f|
      name, rubbish = File.basename(f, ".csv").split('-')
      @data[name] ||= []
      FasterCSV.foreach(f, :headers => true) { |r| @data[name] << r.to_hash }
    end
  end

end

class OutputExperiment < Experiment
  def initialize(dir = nil)
    super(dir)
    load_files Dir.glob(@dir + '/**/*.output.txt');
  end
  
  def extract_sample(pot, row, &conditions)
    sample = []
    @data[pot].each { |d| sample << d[row].to_f if yield d }
    sample
  end
  
private
  def load_files(files)
    group = ""
    
    files.each do |filename|
      File.open(filename) do |file|
        file.each do |line|
          # get header lines
          if line =~ /New experiment with (.*) runs, (.*) robots, (.*) tasks \((.*) per round\)/
            group = "#{$1}-#{$2}-#{$3}-#{$4}"
          end 
          
          @data[group] ||= []
          # get data lines
          if line =~ /Setting: (.*) - (.*), runtime: (.*), total cost: (.*), max cost: (.*)./
            @data[group] << {:wd => $1, :dyn => $2, :runtime => $3, :totalcost => $4, :maxcost => $5 }
          end

        end
      end
    end
  end
  
end

def output_hypos
  x = OutputExperiment.new
  
  samples = []
  samples << {:name =>  "5 runs", :data => x.extract_sample("5-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  samples << {:name => "10 runs", :data => x.extract_sample("10-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  samples << {:name => "15 runs", :data => x.extract_sample("15-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  samples << {:name => "25 runs", :data => x.extract_sample("25-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  samples << {:name => "35 runs", :data => x.extract_sample("35-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  samples << {:name => "50 runs", :data => x.extract_sample("50-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  samples << {:name => "100 runs", :data => x.extract_sample("100-3-15-3", :maxcost) { |r| r[:wd] == "MIN_COST" } }
  x.boxplot(samples)
  sleep(20)
  
end

def csv_hypos
  x = CsvExperiment.new



  # ===============================
  # = MIN COST VS REGRET CLEARING =
  # ===============================

  puts "\n MIN COST VS REGRET CLEARING- STATIC"

  puts "\n\nSTATIC MiniMax Perm 0 - mc max vs rc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 0", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  x.analyse(s1, s2)

  puts "\n\nSTATIC MiniMax Perm 0 - mc time vs rc time"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc time") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 0", "rc time") { |r| r['#tasks/r'] == r['#tasks'] }
  x.analyse(s1, s2, {:paired => true})

  puts "\n\n#######################################################################"

  puts "\n MIN COST VS REGRET CLEARING- RESULTS"


  puts "\n\nMiniMax Perm 0 - mc sum vs rc sum"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc sum")
  s2 = x.extract_sample("MiniMax Perm 0", "rc sum")
  # x.boxplot(s1, s2)
  # sleep(2)
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 - mc max vs rc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc max")
  s2 = x.extract_sample("MiniMax Perm 0", "rc max")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 1 - mc sum vs rc sum"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 1", "mc sum")
  s2 = x.extract_sample("MiniMax Perm 1", "rc sum")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 1 - mc max vs rc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max")
  s2 = x.extract_sample("MiniMax Perm 1", "rc max")
  x.analyse(s1, s2)

  puts "\n\n#######################################################################"

  puts "\n MIN COST VS REGRET CLEARING - RUN TIME "


  puts "\n\nMiniMax Perm 0 - mc time vs rc time"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc time")
  s2 = x.extract_sample("MiniMax Perm 0", "rc time")
  x.analyse(s1, s2, {:paired => true})

  puts "\n\nMiniMax Perm 1 - mc time vs rc time"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 1", "mc time")
  s2 = x.extract_sample("MiniMax Perm 1", "rc time")
  x.analyse(s1, s2, {:paired => true})

  puts "\n\n#######################################################################"


  # ================================
  # = PERM 0 VS PERM 1 VS PERM ALL =
  # ================================
  puts "\n PERM 0 VS PERM 1 VS PERM ALL"

  puts "\n\nMiniMax Perm 0 v 1 - mc sum"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc sum")
  s2 = x.extract_sample("MiniMax Perm 1", "mc sum")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 v 1 - mc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc max")
  s2 = x.extract_sample("MiniMax Perm 1", "mc max")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 v 1 - rc sum"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "rc sum")
  s2 = x.extract_sample("MiniMax Perm 1", "rc sum")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 v 1 - rc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "rc max")
  s2 = x.extract_sample("MiniMax Perm 1", "rc max")
  x.analyse(s1, s2)

  puts "\n\n#######################################################################"

  # ====================
  # = PERM 0 VS PERM 1 =
  # ====================

  puts "\n PERM 0 VS PERM 1 "

  puts "\n\nMiniMax Perm 0 v 1 - mc sum"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc sum")
  s2 = x.extract_sample("MiniMax Perm 1", "mc sum")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 v 1 - mc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "mc max")
  s2 = x.extract_sample("MiniMax Perm 1", "mc max")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 v 1 - rc sum"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "rc sum")
  s2 = x.extract_sample("MiniMax Perm 1", "rc sum")
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 0 v 1 - rc max"
  puts '----------------------------------'
  s1 = x.extract_sample("MiniMax Perm 0", "rc max")
  s2 = x.extract_sample("MiniMax Perm 1", "rc max")
  x.analyse(s1, s2)

  puts "\n\n#######################################################################"


  # =====================
  # = DYNAMIC VS STATIC =
  # =====================

  puts "\n DYNAMIC < STATIC "

  puts "\n\nMiniMax Perm 1, RC, MAX - dyn vs static"
  puts '----------------------------------'
  puts 'vs 5'
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '5' }
  x.analyse(s1, s2)
  puts 'vs 3'
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '3' }
  x.analyse(s1, s2)
  puts 'vs 2'
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '2' }
  x.analyse(s1, s2)
  puts 'vs 1'
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '1' }
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 1, MC, MAX - dyn vs static"
  puts '----------------------------------'
  puts 'vs 5'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '5' }
  x.analyse(s1, s2)
  puts 'vs 3'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '3' }
  x.analyse(s1, s2)
  puts 'vs 2'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '2' }
  x.analyse(s1, s2)
  puts 'vs 1'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '1' }
  x.analyse(s1, s2)
  
  puts "\n\nMiniMax Perm 1, MC, MAX - dyn vs static FOR < 10"
  puts '----------------------------------'
  max = 10
  puts 'vs 5'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] && r['#robots'].to_i < max }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '5' && r['#robots'].to_i < max }
  x.analyse(s1, s2)
  puts 'vs 3'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] && r['#robots'].to_i < max }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '3' && r['#robots'].to_i < max }
  x.analyse(s1, s2)
  puts 'vs 2'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] && r['#robots'].to_i < max }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '2' && r['#robots'].to_i < max }
  x.analyse(s1, s2)
  puts 'vs 1'
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] && r['#robots'].to_i < max }
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '1' && r['#robots'].to_i < max }
  x.analyse(s1, s2)

  # =====================
  # = DYNAMIC VS STATIC =
  # =====================

  puts "\n DYNAMIC > STATIC "

  puts "\n\nMiniMax Perm 1, RC, MAX - dyn vs static"
  puts '----------------------------------'
  puts 'vs 5'
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '5' }
  x.analyse(s1, s2)
  puts 'vs 3'
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '3' }
  x.analyse(s1, s2)
  puts 'vs 2'
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '2' }
  x.analyse(s1, s2)
  puts 'vs 1'
  s2 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "rc max") { |r| r['#tasks/r'] == '1' }
  x.analyse(s1, s2)

  puts "\n\nMiniMax Perm 1, MC, MAX - dyn vs static"
  puts '----------------------------------'
  puts 'vs 5'
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '5' }
  x.analyse(s1, s2)
  puts 'vs 3'
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '3' }
  x.analyse(s1, s2)
  puts 'vs 2'
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '2' }
  x.analyse(s1, s2)
  puts 'vs 1'
  s2 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == r['#tasks'] }
  s1 = x.extract_sample("MiniMax Perm 1", "mc max") { |r| r['#tasks/r'] == '1' }
  x.analyse(s1, s2)
end

csv_hypos
output_hypos

