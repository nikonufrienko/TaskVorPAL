line = input()
line = line.replace("!", " not ").replace("&&", " and ").replace("||", " or ").replace("|", " or ").replace("&", " and ").replace("false", " False ").replace("true", " True ")
n = line.count("X")
arr = []
for i in range(2 ** n):
    line2 = line
    value = bin(i)[2:]
    value = ((n - len(value)) * "0") + value
    for char in value:
        if char == "1":
            line2 = line2.replace("X", " True ", 1)
        else:
            line2 = line2.replace("X", " False ", 1)
    arr.append(eval(line2))
if True in arr and False in arr:
    print("may be true or false")
elif True in arr:
    print("only true")
elif False in arr:
    print("only false")