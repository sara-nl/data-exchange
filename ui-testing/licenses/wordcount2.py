import sys

file = open(sys.argv[1], "rt")
data = file.read()
words = data.split()

print('Number of words in text file :', len(words))
