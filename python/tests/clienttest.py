# -*- coding: utf-8 -*-
import unittest
from fsp.client import Client, AsyncClient
from fsp.client import Task
import tornado.testing
import tornado.concurrent
import logging
import time


class Test(unittest.TestCase):

    def test_parse_host(self):
        client = Client('localhost;127.0.0.1:1111', 'abc', '123')
        self.assertEqual("localhost", client._hosts[0][0])
        # if no port specified, default port should be use, which is 3092
        self.assertEqual(3092, client._hosts[0][1], 'default port is 3092')
        self.assertEqual('127.0.0.1', client._hosts[1][0])
        self.assertEqual(1111, client._hosts[1][1])
        print(client._hosts)

    def testConnect(self):
        client = Client('localhost:3092', 'abc', '123')
        client.connect()
        pass
    
    def test_register_task(self):
        client = Client('localhost:3092', 'abc', '123')
        client.connect()
        task = Task("TestTask", "*/5 * * * * ?")
        
        client.register_task(task, self.task_callback)
    
    def task_callback(self, client, task, instanceId):
        print('task_callback', task)
        client.task_start(instanceId)
        time.sleep(10)
        client.task_running(instanceId)
        time.sleep(10)
        client.task_complete(instanceId)
        time.sleep(10)
        self.stop()
        
    def test_task_fail(self):    
        client = Client('localhost:3092', 'abc', '123')
        client.connect()
        task = Task("TestTask", "*/5 * * * * ?")
        
        client.register_task(task, self.task_callback_report_fail)
        
        
    def task_callback_report_fail(self, client, task, instanceId):
        print('task_callback', task)
        client.task_start(instanceId)
        client.task_running(instanceId)
        client.task_fail(instanceId, "some exception")

class AsyncClientTest(tornado.testing.AsyncTestCase):
    @tornado.testing.gen_test(timeout=30)
    def test_connect(self):
        client = AsyncClient('localhost', 'abc', '123')
        yield client.connect()

    @tornado.testing.gen_test(timeout=30)
    def test_connect_failed(self):
        client = AsyncClient('localhost:99999', '', '')
        try:
            client.connect()
            self.fail("exception not caught")
        except:
            pass

    @tornado.testing.gen_test(timeout=30)
    def test_register_task(self):
        client = AsyncClient('localhost:3092', 'abc', '123')
        yield client.connect()
        task = Task("TestTask", "*/5 * * * * ?")

        client.register_task(task, self.task_callback)

    def task_callback(self, client, task, instanceId):
        print('task_callback', task)
        client.task_start(instanceId)
        time.sleep(10)
        client.task_running(instanceId)
        time.sleep(10)
        client.task_complete(instanceId)
        time.sleep(10)
        self.stop()

    def test_task_fail(self):
        client = Client('localhost:3092', 'abc', '123')
        client.connect()
        task = Task("TestTask", "*/5 * * * * ?")

        client.register_task(task, self.task_callback_report_fail)

    def task_callback_report_fail(self, client, task, instanceId):
        print('task_callback', task)
        client.task_start(instanceId)
        client.task_running(instanceId)
        client.task_fail(instanceId, "some exception")

logging.basicConfig(level=logging.DEBUG)

if __name__ == "__main__":
    import sys;sys.argv = ['', 'Test.test_task_fail']
    unittest.main()