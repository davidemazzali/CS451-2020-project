import socket
import time

byte_message = bytes("Hello, World!", "utf-8")

opened_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

port_num = int(input("port: "))

while(True):
    opened_socket.sendto(byte_message, ("127.0.0.1", port_num))
    time.sleep(100)