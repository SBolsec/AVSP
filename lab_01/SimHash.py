import sys
import hashlib

cache = {}      # Key - int(md5(word)), Value - simhash
hashes = []     # stores calculated hexadecimal simhash values


def simhash(text):
    sh = [0] * 128
    words = text.strip().split(" ")
    for word in words:
        hashed = hashlib.md5(word.encode('utf-8'))
        decimal = int(hashed.hexdigest(), 16)

        if decimal in cache:
            sh = [x + y for x, y in zip(sh, cache[decimal])]
            continue

        bits = format(decimal, '0128b')
        sh_cached = [0] * 128

        for i, bit in enumerate(bits):
            if int(bit) == 1:
                sh[i] += 1
                sh_cached[i] += 1
            else:
                sh[i] -= 1
                sh_cached[i] -= 1

        cache[decimal] = sh_cached

    for i in range(len(sh)):
        if sh[i] >= 0:
            sh[i] = 1
        else:
            sh[i] = 0

    x = ''.join(str(x) for x in sh)
    return int(x, 2)


def count_near_duplicates(line, n):
    fragments = line.split(' ')
    i = int(fragments[0])
    k = int(fragments[1])

    count = 0
    base = hashes[i]

    for x in range(n):
        if x == i:
            continue

        comparing_hash = hashes[x]
        distance = get_hamming_distance(base, comparing_hash)

        if distance <= k:
            count += 1

    return count


def get_hamming_distance(base, comparing_hash):
    if base > comparing_hash:
        t = (comparing_hash, base)
    else:
        t = (base, comparing_hash)

    if t in cache:
        return cache[t]

    distance = hamming_distance(comparing_hash, base)
    cache[t] = distance
    return distance


def hamming_distance(a, b):
    x = a ^ b
    result = 0

    while x > 0:
        result += x & 1
        x >>= 1

    return result


def main():
    n, q, i = None, None, None

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
            hashes.append(simhash(line))
            continue

        print(count_near_duplicates(line, n))


if __name__ == '__main__':
    main()
