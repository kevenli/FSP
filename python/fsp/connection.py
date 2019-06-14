import tornado.gen
import tornado.tcpclient
import tornado.ioloop
import tornado.concurrent
import threading

class TornadoConnection():
    def __init__(self, host, port, on_message_callback, ioloop=None):
        self._host = host
        self._port = port
        self._tcpclient = tornado.tcpclient.TCPClient()
        self._on_message_callback = on_message_callback
        self._ioloop = ioloop
        self.connect_lock = threading.Event()

    def try_readmessage(self):
        pass

    def connect(self):
        '''
        :rtype: None
        :return:
        '''
        connect_future = self._tcpclient.connect(self._host, self._port)
        connect_future.add_done_callback(self.connect_lock.set)
        wait_result = self.connect_lock.wait(30)
        if wait_result == False:
            raise Exception('Connect timeout')
        ex = connect_future.exception()
        if ex:
            raise ex;

        self._socketstream = connect_future.result()
        return

    def close(self):
        self._tcp



