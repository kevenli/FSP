from twisted.internet.protocol import Protocol, ClientFactory
from twisted.internet import reactor
from twisted.internet.protocol import ClientCreator
import protocol.fsp_pb2
import string
import logging
import struct

DEFAULT_PORT = 3092



class FSPClientProtocol(Protocol):
    def connectionMade(self):
        #self.transport.
        logging.debug("connected to host")
        
        
        connect_request = protocol.fsp_pb2.ConnectRequest()
        connect_request.client_protocol_version = "FSP_0.0.1"
        
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.RequestType.CONNECT_REQUEST
        request.Extensions[protocol.fsp_pb2.connect_request] = connect_request
        
        body = request.SerializeToString()
        body_length = len(body)
        length_codec = Base128VarintCodec
        
    
    def send_msg(self, msg):
        data = msg.SerializeToString()
        
        #data_length = 
        
    def compute_varint_length(self, length):
        pass
        
        
    def set_client(self, client):
        
        self._client = client
        

class FSPClientFactory(ClientFactory):
    pass

class Client():
    
    def __init__(self, hosts, app_key, app_secret):
        self._hosts = tuple(self._parse_hosts(hosts))
        
    @staticmethod   
    def _parse_hosts(hosts):
        parts = string.split(hosts, ';')
        for part in parts:
            if string.find(part, ':')>=0:
                host, port = string.split(part, ':', 2)
                port = int(port)
                yield (host, port)
            else:
                yield (part, DEFAULT_PORT)
                
    def _pick_host(self):
        return self._hosts[0]
    
    def got_protocol(self, p):
        logging.debug('connected, got protocol')
        p.set_client(self)
        
    def connect(self):
        host, port = self._pick_host()
        creator = ClientCreator(reactor, FSPClientProtocol)
        logging.debug('connecting %s:%d', host, port)
        creator.connectTCP(host, port).addCallback(self.got_protocol)
        
        reactor.run()
        
class Base128VarintCodec:
    def encode(self, value):
        buffer = ""
        while True:
            if value & ~0x7f == 0:
                buffer += struct.pack('b', value)
                return buffer
            else:
                buffer += struct.pack('B', ((value & 0x7f) | 0x80))
                value = value >> 7 
                
    
    def decode(self, buffer):
        raise NotImplementedError()