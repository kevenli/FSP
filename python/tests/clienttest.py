# -*- coding: utf-8 -*-


'''
Created on 2015年4月10日

@author: hao.li
'''
import unittest
from fsp import Client
from fsp.client import Task
import logging


class Test(unittest.TestCase):

    def test_parse_host(self):
        client = Client('localhost;127.0.0.1:1111', 'abc', '123')
        self.assertEqual("localhost", client._hosts[0][0])
        # if no port specified, default port should be use, which is 3092
        self.assertEqual(3092, client._hosts[0][1], 'default port is 3092')
        self.assertEqual('127.0.0.1', client._hosts[1][0])
        self.assertEqual(1111, client._hosts[1][1])
        print client._hosts

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
        print 'task_callback', task
        client.task_start(instanceId)
        client.task_running(instanceId)
        client.task_complete(instanceId)
        
    def test_task_fail(self):    
        client = Client('localhost:3092', 'abc', '123')
        client.connect()
        task = Task("TestTask", "*/5 * * * * ?")
        
        client.register_task(task, self.task_callback_report_fail)
        
        
    def task_callback_report_fail(self, client, task, instanceId):
        print 'task_callback', task
        client.task_start(instanceId)
        client.task_running(instanceId)
        client.task_fail(instanceId, "some exception")

logging.basicConfig(level=logging.DEBUG)

if __name__ == "__main__":
    import sys;sys.argv = ['', 'Test.test_task_fail']
    unittest.main()