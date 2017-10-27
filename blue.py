import bluetooth
import pam
import base64
#############################################
def authenticate():
    result = False
    sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    sock.bind(("", 1))
    sock.listen(1)
    print("Listening BlueAuth")
    sock.settimeout(60.0)
    csock, address = sock.accept()
    #print("Accepted connection")
    # Send challenge
    #print("Sending challenge")
    csock.send("challenge")
    # Receive response
    response = csock.recv(1024)
    #print("Received: ", response)
    # Send succes or failure
    if(response == b'response'):
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
