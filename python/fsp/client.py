from twisted.internet.protocol import Protocol, ClientFactory
from twisted.internet import reactor
from twisted.internet.protocol import ClientCreator
import protocol.fsp_pb2
import string
import logging
import struct

DEFAULT_PORT = 3092



class FSPClientProtocol(Protocol):
    
    read_buffer = ""
    def connectionMade(self):
        #self.transport.
        logging.debug("connected to host")
        
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.CONNECT_REQUEST
        request.Extensions[protocol.fsp_pb2.connect_request].client_protocol_version="FSP_0.0.1"
        
        self.send_msg(request)
        
    
    def send_msg(self, msg):
        body = msg.SerializeToString()
        body_length = len(body)
        length_codec = Base128VarintCodec();
        encoded_length_package = length_codec.encode(body_length)
        
        self.transport.write(encoded_length_package + body)
        
    def set_client(self, client):
        
        self._client = client
    
    def dataReceived(self, data):
        logging.debug('data received')
        self.read_buffer += data
        while True:
            codec = Base128VarintCodec()
            length_buffer = ""
            for i in range(5):
                if len(self.read_buffer)<i+1:
                    # no enough buffer read, just return wait for next dataReceived
                    return
                length_buffer += self.read_buffer[i]
                if self.read_buffer[i]>0:
                    break
                    
            body_length = codec.decode(length_buffer)
            if len(self.read_buffer) < len(length_buffer) + body_length:
                # no enough buffer
                return
            
            response = protocol.fsp_pb2.Response()
            response.ParseFromString(self.read_buffer[len(length_buffer):len(length_buffer)+body_length])
            self.read_buffer = self.read_buffer[len(length_buffer)+body_length:]
            logging.debug(response)
            self.on_message(response)
            
    
    def on_message(self, response):
        if response.type == protocol.fsp_pb2.Response.CONNECT_RESPONSE:
            self._client.on_connect_response()
        elif response.type == protocol.fsp_pb2.Response.LOGIN_RESPONSE:
            self._client.on_login_response(response.Extensions[protocol.fsp_pb2.login_response])
        
        

class FSPClientFactory(ClientFactory):
    pass

class Client():
    
    def __init__(self, hosts, app_key, app_secret):
        self._hosts = tuple(self._parse_hosts(hosts))
        self._app_key = app_key
        self._app_secret = app_secret
        
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
        self.protocol = p
        
    def connect(self):
        host, port = self._pick_host()
        creator = ClientCreator(reactor, FSPClientProtocol)
        logging.debug('connecting %s:%d', host, port)
        creator.connectTCP(host, port).addCallback(self.got_protocol)
        
        reactor.run()
        
    def on_connect_response(self):
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.LOGIN_REQUEST
        request.Extensions[protocol.fsp_pb2.login_request].app_key=self._app_key
        request.Extensions[protocol.fsp_pb2.login_request].app_secret=self._app_secret
        self.protocol.send_msg(request)
       
    def on_login_response(self, login_response):
        pass
        
        
        
class Base128VarintCodec:
    def encode(self, value):
        data = ""
        while True:
            if value & ~0x7f == 0:
                data += struct.pack('b', value)
                return data
            else:
                data += struct.pack('B', ((value & 0x7f) | 0x80))
                value = value >> 7 
                
    
    def decode(self, data):
        byte_index = 0
        value = 0
        for c in data:
            c_value, = struct.unpack('b', c)
            value |= ((c_value & 0x7f) << (7*byte_index))
            byte_index += 1
            
        return value
            