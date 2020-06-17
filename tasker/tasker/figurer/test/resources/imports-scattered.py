import os
stat_str = os.stat('/tmp')
print(stat_str)

from time import gmtime, strftime
time_str = strftime("%a, %d %b %Y %H:%M:%S +0000", gmtime())

print(time_str)