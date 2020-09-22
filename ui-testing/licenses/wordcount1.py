import sys

from collections import Counter 

file = open(sys.argv[1], "rt")
data = file.read()
words = data.split()

most_common = Counter(words).most_common(3)

print('Three most common words are :', most_common)

print('Number of words in text file :', len(words))
