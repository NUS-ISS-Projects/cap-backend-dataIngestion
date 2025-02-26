# #!python

# __author__ = "DMcG"
# __date__ = "$Jun 23, 2015 10:27:29 AM$"

# import socket
# import time
# from io import BytesIO
# from opendis.DataOutputStream import DataOutputStream
# from opendis.dis7 import EntityStatePdu
# from opendis.RangeCoordinates import GPS, deg2rad

# UDP_PORT = 3000
# DESTINATION_ADDRESS = "127.0.0.1"

# udpSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
# udpSocket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

# gps = GPS()  # conversion helper

# def send():
#     pdu = EntityStatePdu()
#     pdu.pduType = 1
#     pdu.pduStatus = 0  # Set v7-specific PDU status
#     pdu.entityID.entityID = 88
#     pdu.entityID.siteID = 18
#     pdu.entityID.applicationID = 23
#     pdu.marking.setString('Igor3d')
#     pdu.entityAppearance = 0  # Set entity appearance

#     # Set the capabilities field to an integer (e.g., 0 if not used)
#     pdu.capabilities = 0

#     # Entity in Monterey, CA, USA facing North, no roll or pitch
#     montereyLocation = gps.llarpy2ecef(
#         deg2rad(36.6),   # longitude (radians)
#         deg2rad(-121.9), # latitude (radians)
#         1,               # altitude (meters)
#         0,               # roll (radians)
#         0,               # pitch (radians)
#         0                # yaw (radians)
#     )

#     pdu.entityLocation.x = montereyLocation[0]
#     pdu.entityLocation.y = montereyLocation[1]
#     pdu.entityLocation.z = montereyLocation[2]
#     pdu.entityOrientation.psi = montereyLocation[3]
#     pdu.entityOrientation.theta = montereyLocation[4]
#     pdu.entityOrientation.phi = montereyLocation[5]

#     # Set additional required fields for DIS v7
#     pdu.forceId = 1  # Friendly force
#     pdu.entityType.entityKind = 1  # Platform
#     pdu.entityType.domain = 1  # Land
#     pdu.entityType.country = 225  # USA
#     pdu.entityType.category = 1  # Tank
#     pdu.entityType.subcategory = 0
#     pdu.entityType.specific = 0
#     pdu.entityType.extra = 0

#     memoryStream = BytesIO()
#     outputStream = DataOutputStream(memoryStream)
#     pdu.serialize(outputStream)
#     data = memoryStream.getvalue()

#     while True:
#         udpSocket.sendto(data, (DESTINATION_ADDRESS, UDP_PORT))
#         print("Sent {}. {} bytes".format(pdu.__class__.__name__, len(data)))
#         time.sleep(5)
        
# send()



#!/usr/bin/env python3
"""
Extended DIS PDU Simulation Script

This script uses the open-dis-python bindings to craft and send different DIS PDUs over UDP.
It sends:
  - A minimal EntityStatePdu for DIS v6.
  - A minimal EntityStatePdu for DIS v7.
  - A minimal FirePdu for DIS v7 (if available in your opendis package).

Ensure that the opendis package is installed and properly configured.
"""

import socket
import time
from io import BytesIO
from opendis.DataOutputStream import DataOutputStream
from opendis.dis7 import EntityStatePdu, FirePdu
from opendis.RangeCoordinates import GPS, deg2rad

UDP_PORT = 3000
DESTINATION_ADDRESS = "127.0.0.1"

udpSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
udpSocket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

gps = GPS()  # conversion helper

def send_entity_state_pdu(protocol_version):
    """
    Crafts and sends a minimal EntityStatePdu.
    Sets pduStatus to 0 regardless of protocol version to ensure it is an integer.
    """
    pdu = EntityStatePdu()
    pdu.protocolVersion = protocol_version
    pdu.pduType = 1  # Entity State PDU type
    pdu.pduStatus = 0  # Set pduStatus to 0 for both v6 and v7

    # Set entity identification fields
    pdu.entityID.entityID = 88
    pdu.entityID.siteID = 18
    pdu.entityID.applicationID = 23
    pdu.marking.setString("Igor3d")
    pdu.entityAppearance = 0

    pdu.capabilities = 0

    # Set location (using Monterey, CA, USA coordinates)
    montereyLocation = gps.llarpy2ecef(
        deg2rad(36.6),    # longitude (radians)
        deg2rad(-121.9),  # latitude (radians)
        1,                # altitude (meters)
        0,                # roll (radians)
        0,                # pitch (radians)
        0                 # yaw (radians)
    )
    pdu.entityLocation.x = montereyLocation[0]
    pdu.entityLocation.y = montereyLocation[1]
    pdu.entityLocation.z = montereyLocation[2]
    pdu.entityOrientation.psi = montereyLocation[3]
    pdu.entityOrientation.theta = montereyLocation[4]
    pdu.entityOrientation.phi = montereyLocation[5]

    # For DIS v7, set extra fields
    if protocol_version == 7:
        pdu.forceId = 1
        pdu.entityType.entityKind = 1
        pdu.entityType.domain = 1
        pdu.entityType.country = 225
        pdu.entityType.category = 1
        pdu.entityType.subcategory = 0
        pdu.entityType.specific = 0
        pdu.entityType.extra = 0

    # Serialize PDU
    memoryStream = BytesIO()
    outputStream = DataOutputStream(memoryStream)
    pdu.serialize(outputStream)
    data = memoryStream.getvalue()

    udpSocket.sendto(data, (DESTINATION_ADDRESS, UDP_PORT))
    print(f"Sent EntityStatePdu v{protocol_version}. {len(data)} bytes.")

def send_fire_pdu(protocol_version):
    """
    Crafts and sends a minimal FirePdu for DIS v7.
    (This example assumes the FirePdu class is available in opendis.dis7.)
    """
    pdu = FirePdu()
    pdu.protocolVersion = protocol_version
    pdu.pduType = 2  # Example PDU type for FirePdu
    pdu.pduStatus = 0  # Ensure pduStatus is set

    # Set firing entity ID
    pdu.firingEntityID.entityID = 100
    pdu.firingEntityID.siteID = 18
    pdu.firingEntityID.applicationID = 23

    # Set target entity ID
    pdu.targetEntityID.entityID = 200
    pdu.targetEntityID.siteID = 18
    pdu.targetEntityID.applicationID = 23

    # Set munition type (example values)
    pdu.munitionExpendableID.entityID = 1
    pdu.munitionExpendableID.applicationID = 1
    pdu.munitionExpendableID.siteID = 1
    pdu.exerciseID = 1

    memoryStream = BytesIO()
    outputStream = DataOutputStream(memoryStream)
    pdu.serialize(outputStream)
    data = memoryStream.getvalue()

    udpSocket.sendto(data, (DESTINATION_ADDRESS, UDP_PORT))
    print(f"Sent FirePdu v{protocol_version}. {len(data)} bytes.")

def main():
    # Send an EntityStatePdu for DIS v6
    print("Sending DIS EntityStatePdu for v6...")
    send_entity_state_pdu(6)
    time.sleep(5)
    
    # Send an EntityStatePdu for DIS v7
    print("Sending DIS EntityStatePdu for v7...")
    send_entity_state_pdu(7)
    time.sleep(5)
    
    # Send a FirePdu for DIS v7 (example)
    print("Sending DIS FirePdu for v7...")
    send_fire_pdu(7)
    time.sleep(5)

if __name__ == "__main__":
    main()
