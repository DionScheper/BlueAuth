# BlueAuth
BlueAuth is an authentication mechanism that wants to prevent the use of weak passwords.
Strong passwords allow a user to protect data, but remembering these passwords can be a hassle for many en users.
It has been known that people choose weak passwords often just to remember them.

# Security Claim
The token authenticates the user on the machine is the equivalent of having a key to your house.
Provides at least as much security the above statement.
And provides at least as much security as a weak password.

## Security Basis
1. honest token can convice the honest machine that he has the private key to authenticate user
2. dishonest token can not convince the machine to authenticate user
3. honest token cannot be convinced to reveal something by a dishonest machine.

The equivalents to the real world scenarios:
1. Your key will open the door to your house
2. A random key will not open the door and cannot force the door to open
3. Using your key on someone elses door will not enable the owner of that door to copy your key

This works using pam_python:
pip2 install pybluez
