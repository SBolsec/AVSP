import sys
import hashlib


K = 128             # Number of bits in hash
B = 8               # Number of belts
R = int(K / B)      # Number of bits each belt contains

cache = {}          # Cache for sh in simhash
hashes = []         # stores calculated simhash values
candidates = {}     # Candidates for being near duplicates


def simhash(text):
    sh = [0] * 128
    words = text.strip().split(' ')

    for word in words:
        if word in cache:
            sh = [x + y for x, y in zip(sh, cache[word])]
            continue

        hashed = hashlib.md5(word.encode('utf-8'))
        decimal = int(hashed.hexdigest(), 16)

        bits = bin(decimal)[2:].zfill(128)
        sh_cached = [0] * 128

        for i, bit in enumerate(bits):
            if bit == '1':
                sh[i] += 1
                sh_cached[i] += 1
            else:
                sh[i] -= 1
                sh_cached[i] -= 1

        cache[word] = sh_cached

    for i in range(len(sh)):
        if sh[i] >= 0:
            sh[i] = 1
        else:
            sh[i] = 0

    x = ''.join(str(x) for x in sh)
    return int(x, 2)


def count_near_duplicates(line):
    fragments = line.split(' ')
    i = int(fragments[0])
    k = int(fragments[1])

    if i not in candidates:
        return 0

    count = 0
    base_bits = bin(hashes[i])[2:].zfill(128)

    for index in candidates[i]:
        comparing_bits = bin(hashes[index])[2:].zfill(128)

        distance = 0
        for x, y in zip(base_bits, comparing_bits):
            if x != y:
                distance += 1
                if distance > k:
                    break

        if distance <= k:
            count += 1

    return count


def get_hamming_distance(base, comparing_hash):
    if base > comparing_hash:
        pair = (comparing_hash, base)
    else:
        pair = (base, comparing_hash)

    if pair in cache:
        return cache[pair]

    distance = hamming_distance(comparing_hash, base)
    cache[pair] = distance
    return distance


def hamming_distance(a, b):
    x = a ^ b
    result = 0

    while x > 0:
        result += x & 1
        x >>= 1

    return result


def hash_to_int(hash_value, band):
    bits = bin(hash_value)[2:].zfill(128)

    band_end = K - (R * band)
    band_start = band_end - R
    significant_bits = bits[band_start:band_end]

    return int(significant_bits, 2)


def lsh(n):
    for band in range(B):
        compartments = {}

        for current_id in range(n):
            value = hash_to_int(hashes[current_id], band)
            texts_in_compartment = set()

            if value in compartments:
                texts_in_compartment = compartments[value]
                for text_id in texts_in_compartment:
                    if current_id in candidates:
                        candidates[current_id].add(text_id)
                    else:
                        candidates[current_id] = {text_id}

                    if text_id in candidates:
                        candidates[text_id].add(current_id)
                    else:
                        candidates[text_id] = {current_id}
            else:
                texts_in_compartment = set()

            texts_in_compartment.add(current_id)
            compartments[value] = texts_in_compartment


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
            lsh(n)
            continue

        if q is None:
            i -= 1
            hashes.append(simhash(line))
            continue

        print(count_near_duplicates(line))


if __name__ == '__main__':
    main()
