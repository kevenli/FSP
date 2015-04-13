# -*- coding: utf-8 -*-


'''
Created on 2015年4月10日

@author: hao.li
'''
import unittest
from fsp import Client;
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

logging.basicConfig(level=logging.DEBUG)

if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()