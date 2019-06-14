from twisted.internet.protocol import Protocol, ClientFactory
from twisted.internet import reactor
from twisted.internet.protocol import ClientCreator
from .protocol import fsp_pb2
import logging
import struct
import threading
import socket
import time

DEFAULT_PORT = 3092

logger = logging.getLogger(__name__)

class FSPClientProtocol(Protocol):
    
    read_buffer = b""
    def connectionMade(self):
        #self.transport.
        logger.debug("connected to host")
        
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.CONNECT_REQUEST
        request.Extensions[fsp_pb2.connect_request].client_protocol_version="FSP_0.0.1"
        
        self.send_msg(request)
        
    
    def send_msg(self, msg):
        body = msg.SerializeToString()
        body_length = len(body)
        length_codec = Base128VarintCodec()
        encoded_length_package = length_codec.encode(body_length)
        
        self.transport.write(encoded_length_package + body)
        
    def set_client(self, client):
        self._client = client
    
    def dataReceived(self, data):
        logger.debug('data received')
        self.read_buffer += data
        while True:
            codec = Base128VarintCodec()
            length_buffer = b""
            for i in range(5):
                if len(self.read_buffer)<i+1:
                    # no enough buffer read, just return wait for next dataReceived
                    return
                length_buffer += self.read_buffer[i:i+1]
                if self.read_buffer[i]>0:
                    break
                    
            body_length = codec.decode(length_buffer)
            if len(self.read_buffer) < len(length_buffer) + body_length:
                # no enough buffer
                return
            
            response = fsp_pb2.Response()
            response.ParseFromString(self.read_buffer[len(length_buffer):len(length_buffer)+body_length])
            self.read_buffer = self.read_buffer[len(length_buffer)+body_length:]
            logger.debug(response)
            self.on_message(response)
            
    
    def on_message(self, response):
        if response.type == fsp_pb2.Response.CONNECT_RESPONSE:
            self._client.on_connect_response()
        elif response.type == fsp_pb2.Response.LOGIN_RESPONSE:
            self._client.on_login_response(response.Extensions[fsp_pb2.login_response])
        elif response.type == fsp_pb2.Response.REGISTER_TASK_RESPONSE:
            self._client.on_register_response(response.Extensions[fsp_pb2.register_task_response])
        elif response.type == fsp_pb2.Response.TASK_NOTIFY:
            self._client.on_task_notify(response.Extensions[fsp_pb2.task_notify])
        

class FSPClientFactory(ClientFactory):
    pass

CONNECTION_CLOSED = 0
CONNECTION_CONNECTING = 1
CONNECTION_CONNECTED = 2


class Client():
    def __init__(self, hosts, app_key, app_secret):
        self._hosts = tuple(self._parse_hosts(hosts))
        self._app_key = app_key
        self._app_secret = app_secret
        self._task_callbacks = {}
        self._send_msg_queue = []
        self._connection_status = CONNECTION_CLOSED
        self._heartbeat_thread = threading.Thread(target=self.start_heartbeat_loop, daemon=False)

    @staticmethod
    def _parse_hosts(hosts):
        parts = hosts.split(';')
        for part in parts:
            if part.find(':') >= 0:
                host, port = part.split(':', 2)
                port = int(port)
                yield (host, port)
            else:
                yield (part, DEFAULT_PORT)

    def connect(self):
        logger.info('Connecting to server.')
        self._connection_status = CONNECTION_CONNECTING
        if not self._heartbeat_thread.is_alive():
            self._heartbeat_thread.start()
        self._socket = socket.socket()
        address = self._hosts[0]
        self._socket.connect(address)
        self.connectionMade()
        # receive login response
        connect_response = self.receive_next_msg()

        self.on_connect_response()
        login_response = self.receive_next_msg()
        if login_response.Extensions[fsp_pb2.login_response].result_type != fsp_pb2.LoginResponse.SUCCESS:
            raise Exception('Login faild')
        self._connection_status = CONNECTION_CONNECTED

    def _reconnect(self):
        try:
            self.connect()
        except Exception as e:
            logger.warning('Cannot connect to server, connection refused')
            self.connect_disconnected()


    def connectionMade(self):
        # self.transport.
        logger.info("connected")

        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.CONNECT_REQUEST
        request.Extensions[fsp_pb2.connect_request].client_protocol_version = "FSP_0.0.1"

        self.send_msg_directly(request)

    def send_msg_directly(self, msg):
        body = msg.SerializeToString()
        body_length = len(body)
        length_codec = Base128VarintCodec()
        encoded_length_package = length_codec.encode(body_length)
        try:
            self._socket.send(encoded_length_package + body)
            return True
        except (ConnectionResetError, ConnectionRefusedError):
            self.connect_disconnected()
            return False

    def send_msg(self, msg):
        self._send_msg_queue.append(msg)
        while len(self._send_msg_queue)>0:
            next_msg = self._send_msg_queue.pop(0)
            if not self.send_msg_directly(next_msg):
                self._send_msg_queue.insert(0, msg)
                return


    def receive_next_msg(self):
        received_buffer = b''
        codec = Base128VarintCodec()
        try:
            for i in range(5):
                b = self._socket.recv(1)
                received_buffer += b
                if int.from_bytes(b, 'big')>0:
                    break
            body_length = codec.decode(received_buffer)
            msg_binary = self._socket.recv(body_length)
            response = fsp_pb2.Response()
            response.ParseFromString(msg_binary)
            return response
        except (ConnectionResetError, ConnectionRefusedError):
            self.connect_disconnected()

    def on_connect_response(self):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.LOGIN_REQUEST
        request.Extensions[fsp_pb2.login_request].app_key = self._app_key
        request.Extensions[fsp_pb2.login_request].app_secret = self._app_secret
        self.send_msg_directly(request)

    def on_login_response(self, login_response):
        self.connect_lock.set()

    def register_task(self, task, callback):
        self._task_callbacks[task.id] = (task, callback)

        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.REGISTER_TASK
        request.Extensions[fsp_pb2.register_task].task_id = task.id
        request.Extensions[fsp_pb2.register_task].execute_time = task.execute_time
        self.send_msg(request)
        self.receive_next_msg()

    def on_message(self, response):
        logger.debug(response)
        if response.type == fsp_pb2.Response.TASK_NOTIFY:
            self.on_task_notify(response.Extensions[fsp_pb2.task_notify])

    def on_task_notify(self, task_notify):
        task, task_callback = self._task_callbacks[task_notify.task_id]
        self.task_start(task_notify.task_instance_id)
        try:
            task_callback(task)
            self.task_complete(task_notify.task_instance_id)
        except Exception as e:
            self.task_fail(task_notify.task_instance_id, str(e))

    def task_start(self, instance_id):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id = instance_id
        request.Extensions[fsp_pb2.task_status_update].status = fsp_pb2.TaskStatusUpdate.START
        self.send_msg(request)

    def task_running(self, instance_id, progress=0):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id = instance_id
        request.Extensions[fsp_pb2.task_status_update].status = fsp_pb2.TaskStatusUpdate.RUNNING
        request.Extensions[fsp_pb2.task_status_update].percentage = progress
        self.send_msg(request)

    def task_complete(self, instance_id):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id = instance_id
        request.Extensions[fsp_pb2.task_status_update].status = fsp_pb2.TaskStatusUpdate.COMPLETE
        self.send_msg(request)

    def task_fail(self, instance_id, error_message):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id = instance_id
        request.Extensions[fsp_pb2.task_status_update].status = fsp_pb2.TaskStatusUpdate.FAILED
        request.Extensions[fsp_pb2.task_status_update].error_message = error_message
        self.send_msg(request)

    def wait_till_end(self):
        while True:
            time.sleep(0.001)
            if self._connection_status == CONNECTION_CONNECTED:
                response = self.receive_next_msg()
                # if connection lost, response might be none
                if response:
                    self.on_message(response)

    def start_heartbeat_loop(self):
        while True:
            time.sleep(10)
            if self._connection_status == CONNECTION_CONNECTED:
                self.send_heartbeat()
            elif self._connection_status == CONNECTION_CLOSED:
                self._reconnect()

    def send_heartbeat(self):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.HEARTBEAT
        self.send_msg(request)

    def connect_disconnected(self):
        logger.warning('Connection lost')
        self._connection_status = CONNECTION_CLOSED


class TwistedClient():
    def __init__(self, hosts, app_key, app_secret):
        super(TwistedClient, self).__init__(hosts, app_key, app_secret)

    def _pick_host(self):
        return self._hosts[0]
    
    def got_protocol(self, p):
        logger.debug('connected, got protocol')
        p.set_client(self)
        self.protocol = p
        
    def connect(self):
        host, port = self._pick_host()
        creator = ClientCreator(reactor, FSPClientProtocol)
        logger.debug('connecting %s:%d', host, port)
        creator.connectTCP(host, port).addCallback(self.got_protocol)
        
        self.trasport_thread = threading.Thread(target=reactor.run, args=(False,)).start()
        #reactor.run()
        self.connect_lock.wait()
        
    def register_task(self, task, callback):
        self.task_callbacks[task.id] = (task, callback)
        
        request = fsp_pb2.Request() 
        request.type = fsp_pb2.Request.REGISTER_TASK
        request.Extensions[fsp_pb2.register_task].task_id=task.id
        request.Extensions[fsp_pb2.register_task].execute_time=task.execute_time
        self.protocol.send_msg(request)
        
    
    def unregister_task(self, task):
        self.task_callbacks.pop(task.id)
        
    def task_start(self, instance_id):
        request = fsp_pb2.Request() 
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[fsp_pb2.task_status_update].status=fsp_pb2.TaskStatusUpdate.START
        self.protocol.send_msg(request)

    def task_running(self, instance_id, progress=0):
        request = fsp_pb2.Request() 
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[fsp_pb2.task_status_update].status=fsp_pb2.TaskStatusUpdate.RUNNING
        request.Extensions[fsp_pb2.task_status_update].percentage=progress
        self.protocol.send_msg(request)
    
    def task_complete(self, instance_id):
        request = fsp_pb2.Request() 
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[fsp_pb2.task_status_update].status=fsp_pb2.TaskStatusUpdate.COMPLETE
        self.protocol.send_msg(request)
    
    def task_fail(self, instance_id, error_message):
        request = fsp_pb2.Request() 
        request.type = fsp_pb2.Request.TASK_STATUS_UPDATE
        request.Extensions[fsp_pb2.task_status_update].instance_id=instance_id
        request.Extensions[fsp_pb2.task_status_update].status=fsp_pb2.TaskStatusUpdate.FAILED
        request.Extensions[fsp_pb2.task_status_update].error_message=error_message
        self.protocol.send_msg(request)
        
    def on_connect_response(self):
        request = fsp_pb2.Request() 
        request.type = fsp_pb2.Request.LOGIN_REQUEST
        request.Extensions[fsp_pb2.login_request].app_key=self._app_key
        request.Extensions[fsp_pb2.login_request].app_secret=self._app_secret
        self.protocol.send_msg(request)
       
    def on_login_response(self, login_response):
        self.connect_lock.set()
        
    def on_register_response(self, register_response):
        pass
    
    def on_task_notify(self, task_notify):
        task, task_callback = self.task_callbacks[task_notify.task_id]
        task_callback(self, task, task_notify.task_instance_id)

    def close(self):
        pass

    def __del__(self):
        pass
        
class Task:
    def __init__(self, task_id, execute_time):
        self.id = task_id
        self.execute_time = execute_time
        
        
class Base128VarintCodec:
    def encode(self, value):
        data = b""
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
        for c_value in data:
            value |= ((c_value & 0x7f) << (7*byte_index))
            byte_index += 1
            
        return value


import tornado.gen
import tornado.tcpclient
import tornado.ioloop
import tornado.concurrent

class AsyncClient(Client):
    def __init__(self, hosts, app_key, app_secret, ioloop=None):
        self._hosts = tuple(self._parse_hosts(hosts))
        self._app_key = app_key
        self._app_secret = app_secret
        self._tcpclient = tornado.tcpclient.TCPClient()
        self.connect_lock = threading.Event()
        if ioloop is None:
            self._ioloop = tornado.ioloop.IOLoop.current()
        else:
            self._ioloop = ioloop

        self._receive_callback = tornado.ioloop.PeriodicCallback(self._receive, 0.001)
        self.read_buffer = b''
        self.connect_callback = None

    def _receive_done(self, future):
        try:
            result = future.result()
            self._data_received(result)
        finally:
            self._receive()


    def _data_received(self, data):
        self.read_buffer += data
        while True:
            codec = Base128VarintCodec()
            length_buffer = b""
            for i in range(5):
                if len(self.read_buffer) < i + 1:
                    # no enough buffer read, just return wait for next dataReceived
                    return
                length_buffer += self.read_buffer[i:i + 1]
                if self.read_buffer[i] > 0:
                    break

            body_length = codec.decode(length_buffer)
            if len(self.read_buffer) < len(length_buffer) + body_length:
                # no enough buffer
                return

            response = fsp_pb2.Response()
            response.ParseFromString(self.read_buffer[len(length_buffer):len(length_buffer) + body_length])
            self.read_buffer = self.read_buffer[len(length_buffer) + body_length:]
            logger.debug(response)
            self.on_message(response)

    def _receive(self):
        logger.debug('data received')
        #read_future = self._socketstream.read_into(self.read_buffer, partial=True)
        read_future = self._socketstream.read_bytes(8192, partial=True)
        read_future.add_done_callback(self._receive_done)
        return



    def on_message(self, response):
        logger.debug(response)
        if response.type == fsp_pb2.Response.CONNECT_RESPONSE:
            self.on_connect_response()
        elif response.type == fsp_pb2.Response.LOGIN_RESPONSE:
            self.on_login_response(response.Extensions[fsp_pb2.login_response])
        # elif response.type == fsp_pb2.Response.REGISTER_TASK_RESPONSE:
        #     self._client.on_register_response(response.Extensions[fsp_pb2.register_task_response])
        # elif response.type == fsp_pb2.Response.TASK_NOTIFY:
        #     self._client.on_task_notify(response.Extensions[fsp_pb2.task_notify])

    def on_login_response(self, login_response):
        if self.connect_callback:
            self.connect_callback()
        self._connect_future.set_result('true')

    def on_connect_response(self):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.LOGIN_REQUEST
        request.Extensions[fsp_pb2.login_request].app_key=self._app_key
        request.Extensions[fsp_pb2.login_request].app_secret=self._app_secret
        self.send_msg(request)

    @tornado.gen.coroutine
    def connect(self, callback=None):
        hosts = self._hosts
        connect_host = hosts[0][0]
        connect_port = hosts[0][1]

        if callback:
            self.connect_callback=callback

        self._socketstream = yield self._tcpclient.connect(connect_host, connect_port)
        self._socketstream.set_nodelay(True)
        self.send_connect_request()
        self._connect_future = tornado.ioloop.Future()
        #self._receive_callback.start()
        self._receive()
        #self.connect_lock.wait(30)
        yield self._connect_future
        raise tornado.gen.Return()

    def send_connect_request(self):
        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.CONNECT_REQUEST
        request.Extensions[fsp_pb2.connect_request].client_protocol_version="FSP_0.0.1"
        self.send_msg(request)

    def send_msg(self, msg):
        body = msg.SerializeToString()
        body_length = len(body)
        length_codec = Base128VarintCodec()
        encoded_length_package = length_codec.encode(body_length)

        self._socketstream.write(encoded_length_package + body)

    def start(self):
        self._ioloop.start()

    def register_task(self, task, callback):
        self.task_callbacks[task.id] = (task, callback)

        request = fsp_pb2.Request()
        request.type = fsp_pb2.Request.REGISTER_TASK
        request.Extensions[fsp_pb2.register_task].task_id = task.id
        request.Extensions[fsp_pb2.register_task].execute_time = task.execute_time
        self.send_msg(request)
