import bluetooth
import random
import subprocess
from binascii import unhexlify
from binascii import hexlify
############## IMPORTED PUBLIC KEYS ##############
pubkey = dict()
modulus = dict()
##################################################
def to_bytes (val):
    width = val.bit_length()
    width += 8 - ((width % 8) or 8)
    fmt = '%%0%dx' % (width // 4)
    s = unhexlify(fmt % val)
    return s

def from_bytes(val):
    return int(hexlify(val), 16)

def authenticate(username):
    bt_enabled = subprocess.check_output(["bluetooth"])
    if not "on" in bt_enabled.split():
        print("Bluetooth is Disabled")
        return False
    result = False
    sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    sock.bind(("", 1))
    sock.listen(1)
    print("Listening BlueAuth")
    sock.settimeout(20.0)
    csock, address = sock.accept()
    #print("Accepted connection")
    # Send challenge
    challenge = random.getrandbits(1024)
    #print("Challenge: %s\n\n" % challenge)
    csock.send(to_bytes(challenge))
    # Receive response
    response = from_bytes(csock.recv(1024))
    #print("Response: %s\n\n" % response)
    #print("Pubkey: %s\n\n" % pubkey[username])
    #print("Modulus: %s\n\n" % modulus[username])
    answer = pow(response, pubkey[username], modulus[username])
    #print("Answer: %s" % answer)
    # Send succes or failure
    if(answer == challenge):
        print("Succesful challenge response")
        result = True
        csock.send("y")
    else:
        print("Failure challenge response")
        csock.send("n")
    csock.close()
    sock.close()
    return result

###############################################
def pam_sm_authenticate(pamh, flags, argv):
    #
    # Get the user name.
    #
    try:
        username = pamh.get_user()
    except pamh.exception:
        username = None
    if username == None or pubkey[username] == None:
        return pamh.PAM_AUTH_ERR
    #
    # User exists and has public key, authenticate
    #
    if authenticate(username):
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
    if(authenticate('neonlight') == True):
        print("Succes")
    else:
        print("Failure")
