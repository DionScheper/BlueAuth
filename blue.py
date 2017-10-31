import bluetooth
import random
import subprocess
from binascii import unhexlify
from binascii import hexlify
############## IMPORTED PUBLIC KEYS ##############
pubkey = dict()
modulus = dict()
pubkey['neonlight'] = 65537
pubkey['irrlicht'] = 65537
modulus['neonlight'] = 27782227073481062722382380058804882626841903870284389816229665127306044133457658666558756806542012814119177605869416405832956398148254110975028078058376150244336322723396782414961782972868150556402980507944803414676509927002882817109108324214648717897636823574794616829861959691617956594479947718050402210898492235359861568123313948674330253703743046920439614738110269456849920061600874091813533006909503951422942198522112268339576974305850590062593135745046816677601811403158793537300580141696183617119051289008416737863461991269522059719165607570775991294475681108819918363416183477317772174188151721211641017896841
modulus['irrlicht'] = 23389052776153011543224271980766301143493807242368939848404659091296689181301365074002282520205250954914375816943928759732010657624577665429213170844192555089758092940572100475343270274695726729265328162649274497264209165712171026863816584298768409110499469900145117409266272026239084643311941109108793837934685512127582078788847983297594155865073370990874698934183545179940937106334525202037703033722251709445970175799084785539291752496572907962123963328803230901856372152952237114721530214901693574328933223674749606795350172275572401619543852954504897469099215016720800679739067814591507611915594792861834904841077
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
