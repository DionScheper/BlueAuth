#modulus = 125069900423969625513167183696456491266355822058428405013171349998050773625405886598592049594275340709050366945325919003276151192237618263657205453525233716561923402213695623567778362333117173702622386572910559830547887242097802023428298910714674944609028413754889990570586265421100578971326792564496946845503

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
