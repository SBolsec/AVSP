import sys
import hashlib
import numpy as np


def simhash(cache, text):
    sh = np.zeros(128, dtype=int)
    words = text.strip().split(' ')

    for word in words:
        if word in cache:
            sh += cache[word]
            continue

        hashed = hashlib.md5(word.encode('utf-8'))
        decimal = int(hashed.hexdigest(), 16)

        bits = bin(decimal)[2:].zfill(128)
        sh_cached = np.zeros(128, dtype=int)

        for i, bit in enumerate(bits):
            if bit == '1':
                sh_cached[i] += 1
            else:
                sh_cached[i] -= 1

        sh += sh_cached
        cache[word] = sh_cached

    sh = [1 if x >= 0 else 0 for x in sh]
    x = ''.join(str(x) for x in sh)
    return int(x, 2)


bits_in_hexadecimal = {
    '0': 0, '1': 1, '2': 1, '3': 2, '4': 1, '5': 2, '6': 2, '7': 3,
    '8': 1, '9': 2, 'a': 2, 'b': 3, 'c': 2, 'd': 3, 'e': 3, 'f': 4,
}


def count_near_duplicates(hashes, line, n):
    fragments = line.split(' ')
    i = int(fragments[0])
    k = int(fragments[1])

    count = 0

    for index in range(n):
        if index == i:
            continue

        distance = 0
        for x in hex(hashes[i] ^ hashes[index])[2:]:
            distance += bits_in_hexadecimal[x]
            if distance > k:
                break

        if distance <= k:
            count += 1

    return count


def main():
    n, q, i = None, None, None
    hashes = []
    cache = {}

    for line in sys.stdin:
        if n is None:
            n = int(line)
            i = n
            continue

        if i == 0:
            q = int(line)
            i = q
            continue

        if q is None:
            i -= 1
            hashes.append(simhash(cache, line))
            continue

        print(count_near_duplicates(hashes, line, n))


if __name__ == '__main__':
    main()
