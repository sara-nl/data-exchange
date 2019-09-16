import json


def average(lst):
    return sum(lst) / len(lst)


print("Good code starting")

json_file = open("/tmp/input/1.json", 'r')
json_text = json_file.read()
json_obj = json.loads(json_text)

result = average(json_obj['items'])

f = open("/tmp/output/good-code.txt", "w")
f.write(str(result))
f.close()
