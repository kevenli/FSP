# -*- coding: utf-8 -*-
'''
Created on 2015年4月10日

@author: hao.li
'''
import unittest
from fsp.client import Base128VarintCodec


class Test(unittest.TestCase):


    def test_encode_varint32_1(self):
        i = 1
        codec = Base128VarintCodec()
        actual = codec.encode(i)
        self.assertEqual('\x01', actual)
        
    def test_encode_varint32_127(self):
        i = 127
        codec = Base128VarintCodec()
        actual = codec.encode(i)
        self.assertEqual('\x7F', actual)
        
    def test_encode_varint32_128(self):
        i = 128
        codec = Base128VarintCodec()
        actual = codec.encode(i)
        self.assertEqual('\x80\x01', actual)
        
    def test_encode_varint32_255(self):
        i = 255
        codec = Base128VarintCodec()
        actual = codec.encode(i)
        self.assertEqual('\xff\x01', actual)
        
    def test_encode_varint32_256(self):
        i = 256
        codec = Base128VarintCodec()
        actual = codec.encode(i)
        self.assertEqual('\x80\x02', actual)
        


if __name__ == "__main__":
    import sys;sys.argv = ['', 'Test.test_encode_varint32_256']
    unittest.main()