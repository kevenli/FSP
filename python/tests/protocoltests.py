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
        
    def test_decode_varint32_1(self):
        i = 1
        data = '\x01'
        codec = Base128VarintCodec()
        actual = codec.decode(data)
        self.assertEqual(1, actual)
        
    def test_decode_varint32_127(self):
        data = '\x7F'
        value = 127
        codec = Base128VarintCodec()
        actual = codec.decode(data)
        self.assertEqual(value, actual)
        
    def test_decode_varint32_128(self):
        data = '\x80\x01'
        value = 128
        codec = Base128VarintCodec()
        actual = codec.decode(data)
        self.assertEqual(value, actual)
        
    def test_decode_varint32_255(self):
        data = '\xff\x01'
        value = 255
        codec = Base128VarintCodec()
        actual = codec.decode(data)
        self.assertEqual(value, actual)
        
    def test_decode_varint32_256(self):
        data = '\x80\x02'
        value = 256
        codec = Base128VarintCodec()
        actual = codec.decode(data)
        self.assertEqual(value, actual)


if __name__ == "__main__":
    import sys;sys.argv = ['', 'Test.test_decode_varint32_1']
    unittest.main()