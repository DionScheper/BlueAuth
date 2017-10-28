import bluetooth
import random
import subprocess
from binascii import unhexlify
from binascii import hexlify
#############################################
pubkey = 65537
modulus = 125069900423969625513167183696456491266355822058428405013171349998050773625405886598592049594275340709050366945325919003276151192237618263657205453525233716561923402213695623567778362333117173702622386572910559830547887242097802023428298910714674944609028413754889990570586265421100578971326792564496946845503
#############################################
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

def authenticate():
    bt_enabled = subprocess.check_output(["hcitool", "dev"])
    if not "hci0" in bt_enabled.split():
        print("Bluetooth is Disabled")
        return False
    result = False
    sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    sock.bind(("", 1))
    sock.listen(1)
    print("Listening BlueAuth")
    sock.settimeout(60.0)
    csock, address = sock.accept()
    #print("Accepted connection")
    # Send challenge
    challenge = random.getrandbits(256)
    print("Challenge: %s" % challenge)
    csock.send(bytes(challenge))
    # Receive response
    response = from_bytes(csock.recv(256))
    print("Response: %s\n\n" % response)
    print("Pubkey: %s\n\n" % pubkey)
    print("Modulus: %s\n\n" % modulus)
    answer = pow(response, pubkey, modulus)
    print("Answer: %s" % answer)
    # Send succes or failure
    if(challenge == answer):
        #print("Succesful challenge response")
        result = True
        csock.send("y")
    else:
        #print("Failure challenge response")
        csock.send("n")
    csock.close()
    sock.close()
    return result
###############################################
def pam_sm_authenticate(pamh, flags, argv):
    if authenticate():
        return pamh.PAM_SUCCESS
    return pamh.PAM_AUTH_ERR

def pam_sm_setcred(pamh, flags, argv):
    return pamh.PAM_SUCCESS

def pam_sm_acct_mgmt(pamh, flags, argv):
    return pamh.PAM_SUCCESS

def pam_sm_open_session(pamh, flags, argv):
    return pamh.PAM_SUCCESS

def pam_sm_close_session(pamh, flags, argv):
    return pamh.PAM_SUCCESS

def pam_sm_chauthtok(pamh, flags, argv):
    return pamh.PAM_SUCCESS

if __name__ == "__main__":
    if(authenticate() == True):
        print("Succes")
    else:
        print("Failure")
