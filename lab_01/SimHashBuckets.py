import sys
import hashlib
import time


K = 128             # Number of bits in hash
B = 8               # Number of belts
R = int(K / B)      # Number of bits each belt contains

def simhash(cache, text):
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

    sh = [1 if x >= 0 else 0 for x in sh]
    x = ''.join(str(x) for x in sh)
    return int(x, 2)


bits_in_hexadecimal = {
    '0': 0, '1': 1, '2': 1, '3': 2, '4': 1, '5': 2, '6': 2, '7': 3,
    '8': 1, '9': 2, 'a': 2, 'b': 3, 'c': 2, 'd': 3, 'e': 3, 'f': 4,
}


def count_near_duplicates(hashes, candidates, line):
    fragments = line.split(' ')
    i = int(fragments[0])
    k = int(fragments[1])

    if i not in candidates:
        return 0

    count = 0

    for index in candidates[i]:
        distance = 0
        for x in hex(hashes[i] ^ hashes[index])[2:]:
            distance += bits_in_hexadecimal[x]
            if distance > k:
                break

        if distance <= k:
            count += 1

    return count


def hash_to_int(hash_value, band):
    bits = bin(hash_value)[2:].zfill(128)

    band_end = K - (R * band)
    band_start = band_end - R
    significant_bits = bits[band_start:band_end]

    return int(significant_bits, 2)


def lsh(hashes, n):
    candidates = {}

    for band in range(B):
        compartments = {}

        for current_id in range(n):
            value = hash_to_int(hashes[current_id], band)

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

    return candidates


def main():
    n, q, i, candidates = None, None, None, None
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
            candidates = lsh(hashes, n)
            continue

        if q is None:
            i -= 1
            hashes.append(simhash(cache, line))
            continue

        print(count_near_duplicates(hashes, candidates, line))


if __name__ == '__main__':
    start = time.time()
    main()
    end = time.time()
    print(end - start)
