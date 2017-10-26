import bluetooth
import pam

def discover_device(device_name):
    print("Discovering devices...")
    nearby_devices = bluetooth.discover_devices()
    for bdaddr in nearby_devices:
        print(bluetooth.lookup_name( bdaddr ) + ":::" + bdaddr)
        if(bdaddr == device_name):
            return True
    return False


def connect_to(phone, port):
    sock = bluetooth.BluetoothSocket( bluetooth.RFCOMM )
    sock.connect((phone, port))

iphone = "5C:97:F3:50:10:28"
experia="58:48:22:00:2E:5A"
con_name = "BlueAuth"
uuid = "c03d88b8-d291-4e7e-b608-b68a2367329d"


# result_dict = bluetooth.find_service(name=con_name,uuid=uuid,address=experia)
# if(len(result_dict) == 1):
#     port = result_dict[0]['port']
#     connect_to(experia, port)
#
#

#############################################
sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
sock.bind(("", 1))
sock.listen(1)
print("Listening")
sock.settimeout(60.0)
csock, address = sock.accept()
print("Accepted connection")
# Send challenge
csock.send("challenge")
# Receive response
response = csock.recv(1024)
# Send succes or failure
if(response == "response"):
    print("Succesful challenge response")
else:
    print("Failure challenge response")
csock.close()
sock.close()

print("final")
