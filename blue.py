import bluetooth
import random
import subprocess
from binascii import unhexlify
from binascii import hexlify
############## IMPORTED PUBLIC KEYS ##############
pubkey = dict()
modulus = dict()
pubkey['neonlight'] = 65537
modulus['neonlight'] = 25249845793627115865829824562353021364149415877339799811707391357065515484755700220558876841837514256873724948929535352208897399966862372860282064519886974572724055712409217507698243807937081228744832461657361358817433380998283198495245713169781781989002057167927968385114120405123669859723261526746992761655297979968841798242899496896657341200118549645488640613807323718595786402778004501569513589677657266297498570110862832585237153725739886064271096652440118597449242000001560823470929294626974825473306365309438281968983663341753592104257353576075126392948207782011001487601553932207087655160020868933580945880787
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
