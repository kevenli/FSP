from twisted.internet.protocol import Protocol, ClientFactory
from twisted.internet import reactor
from twisted.internet.protocol import ClientCreator
import protocol.fsp_pb2
import string
import logging
import struct
import threading

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
        elif response.type == protocol.fsp_pb2.Response.REGISTER_TASK_RESPONSE:
            self._client.on_register_response(response.Extensions[protocol.fsp_pb2.register_task_response])
        elif response.type == protocol.fsp_pb2.Response.TASK_NOTIFY:
            self._client.on_task_notify(response.Extensions[protocol.fsp_pb2.task_notify])
        

class FSPClientFactory(ClientFactory):
    pass

class Client():
    
    task_callbacks = {}
    
    def __init__(self, hosts, app_key, app_secret):
        self._hosts = tuple(self._parse_hosts(hosts))
        self._app_key = app_key
        self._app_secret = app_secret
        self.connect_lock = threading.Event()
        
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
        
        self.trasport_thread = threading.Thread(target=reactor.run, args=(False,)).start()
        #reactor.run()
        self.connect_lock.wait()
        
    def register_task(self, task, callback):
        self.task_callbacks[task.id] = (task, callback)
        
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.REGISTER_TASK
        request.Extensions[protocol.fsp_pb2.register_task].task_id=task.id
        request.Extensions[protocol.fsp_pb2.register_task].execute_time=task.execute_time
        self.protocol.send_msg(request)
        
    
    def unregister_task(self, task):
        self.task_callbacks.pop(task.id)
        
    def task_start(self, instance_id):
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[protocol.fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[protocol.fsp_pb2.task_status_update].status=protocol.fsp_pb2.TaskStatusUpdate.START
        self.protocol.send_msg(request)

    def task_running(self, instance_id, progress=0):
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[protocol.fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[protocol.fsp_pb2.task_status_update].status=protocol.fsp_pb2.TaskStatusUpdate.RUNNING
        request.Extensions[protocol.fsp_pb2.task_status_update].percentage=progress
        self.protocol.send_msg(request)
    
    def task_complete(self, instance_id):
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[protocol.fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[protocol.fsp_pb2.task_status_update].status=protocol.fsp_pb2.TaskStatusUpdate.COMPLETE
        self.protocol.send_msg(request)
    
    def task_fail(self, instance_id, error_message):
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[protocol.fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[protocol.fsp_pb2.task_status_update].status=protocol.fsp_pb2.TaskStatusUpdate.FAILED
        request.Extensions[protocol.fsp_pb2.task_status_update].error_message=error_message
        self.protocol.send_msg(request)
        
    def on_connect_response(self):
        request = protocol.fsp_pb2.Request() 
        request.type = protocol.fsp_pb2.Request.LOGIN_REQUEST
        request.Extensions[protocol.fsp_pb2.login_request].app_key=self._app_key
        request.Extensions[protocol.fsp_pb2.login_request].app_secret=self._app_secret
        self.protocol.send_msg(request)
       
    def on_login_response(self, login_response):
        self.connect_lock.set()
        
    def on_register_response(self, register_response):
        pass
    
    def on_task_notify(self, task_notify):
        task, task_callback = self.task_callbacks[task_notify.task_id]
        task_callback(self, task, task_notify.task_instance_id)
        
class Task:
    def __init__(self, task_id, execute_time):
        self.id = task_id
        self.execute_time = execute_time
        
        
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
            