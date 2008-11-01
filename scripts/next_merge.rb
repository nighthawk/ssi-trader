def fac n
  if n <= 1
    1
  else
    n * fac(n-1)
  end
end

class Array
  def permut flex, &block
    total = fac(self.size + flex.size) / fac(self.size)

    (1..total).each do |count|
      fix = self
      n = fix.size
      
      (0..flex.size-1).each do |i|
        magic = (n + i + 1)
        at = count % magic
        puts "#{i} to be inserted at #{at} (#{count} % #{n} + #{i} + 1, i.e. #{count} % #{magic})"

        fix_l = at > 0 ? fix[0..at-1] : []
        fix_r = at < fix.size ? fix [at..-1] : []

        fix = fix_l + flex[i..i] + fix_r
      end
      yield fix
    end
  end
end



def a_print array
  puts array.join(", ")
end

=begin
["a", "b"].permut ["1", "2"] do |t| a_print t end
puts "_______________________________"
["a", "b", "c"].permut [] do |t| a_print t end
puts "_______________________________"
[].permut ["a", "b", "c"] do |t| a_print t end
puts "_______________________________"
["a", "b", "c"].permut ["1", "2", "3"] do |t| a_print t end
=end

# [].permut ["-6 -5", "-7 7", "-5 -1", "11 -8"] do |t| a_print t end
# ["a"]

array = []
#[].permut ["1", "2", "3", "4"] do |t|
["-7 7", "-5 -1"].permut ["-6 -5", "11 -8"] do |t|
  array << t.join(", ")
  a_print t
end
puts "######################"
if array.size - array.uniq.size > 0
  puts array.sort
  puts "ERROR by #{array.size - array.uniq.size}"
else
  puts "FINE"
end

