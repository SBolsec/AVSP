import sys
from math import floor
from itertools import combinations


def first_pass():
    baskets = []
    item_count = {}

    for line in sys.stdin:
        items = [int(x) for x in line.strip().split(' ')]
        baskets.append(items)

        for item in items:
            item_count[item] = item_count.get(item, 0) + 1

    return baskets, item_count, len(item_count)


def second_pass(baskets, item_count, item_count_len, prag, b):
    pretinci = {}
    for basket in baskets:
        for i, j in combinations(basket, r=2):
            if item_count[i] >= prag and item_count[j] >= prag:
                k = (i * item_count_len + j) % b
                pretinci[k] = pretinci.get(k, 0) + 1

    return pretinci


def third_pass(baskets, item_count, item_count_len, prag, b, pretinci):
    parovi = {}
    for basket in baskets:
        for i, j in combinations(basket, r=2):
            if item_count[i] >= prag and item_count[j] >= prag:
                k = (i * item_count_len + j) % b
                if pretinci[k] >= prag:
                    parovi[(i, j)] = parovi.get((i, j), 0) + 1

    return parovi


def main():
    n = int(sys.stdin.readline())  # broj kosara
    s = float(sys.stdin.readline())  # prag podrske
    b = int(sys.stdin.readline())  # broj pretinaca
    prag = floor(s * n)

    baskets, item_count, item_count_len = first_pass()
    pretinci = second_pass(baskets, item_count, item_count_len, prag, b)
    parovi = third_pass(baskets, item_count, item_count_len, prag, b, pretinci)

    print(len(pretinci))
    print(len(parovi))
    [print(parovi[i]) for i in sorted(parovi, key=parovi.get, reverse=True)]


if __name__ == '__main__':
    main()
