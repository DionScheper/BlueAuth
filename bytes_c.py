from binascii import unhexlify
from binascii import hexlify

def to_bytes (val, endianness='big'):
    width = val.bit_length()
    width += 8 - ((width % 8) or 8)
    fmt = '%%0%dx' % (width // 4)
    s = unhexlify(fmt % val)
    if endianness == 'little':
        s = s[::-1]
    return s

def from_bytes(val):
    return int(hexlify(val), 16)

import random
for i in range(20):
    challenge = random.getrandbits(256)

    # Python2
    tmp2 = to_bytes(challenge)
    resp2 = from_bytes(tmp2)

    if(challenge != resp2):
        print("Not equal")
