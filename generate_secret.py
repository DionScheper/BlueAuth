from Crypto.PublicKey import RSA

rsa_key = RSA.generate(1024)

print("PUBLIC:")
print(getattr(rsa_key, 'e'))
print("PRIVATE:")
print(getattr(rsa_key, 'd'))
print("MODULUS:")
print(getattr(rsa_key, 'n'))
