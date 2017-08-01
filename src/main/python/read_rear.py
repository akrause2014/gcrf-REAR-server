import struct

import numpy as np

def get_visitor_func():
    return {
        1: read_sensor,
        2: read_sensor,
        3: read_sensor,
        4: read_location,
        5: read_time,
        6: read_location
    }
    
def read_time(f, entry_type, timestamp, visitor):
    systemtime, = struct.unpack('>q', f.read(8))
    visitor.visit_time(timestamp, systemtime)

def read_sensor(f, entry_type, timestamp, visitor):
    x, y, z = struct.unpack('>fff', f.read(12))
    visitor.visit_sensor(timestamp, entry_type, x, y, z)
    
def read_location(f, entry_type, timestamp, visitor):
    lat,lon,alt,acc = struct.unpack('>dddf', f.read(28))
    visitor.visit_location(timestamp, entry_type, lat, lon, alt, acc)

def read_data(file_name, visitor):
    vis_func = get_visitor_func()
    with open(file_name, 'rb') as f:
        while True:
            data = f.read(10)
            if not data:
                break
            version, entry_type, timestamp = struct.unpack('> b b q', data)
            vis_func[entry_type](f, entry_type, timestamp, visitor)


class DataVisitor(object):
    
    def visit_time(self, timestamp, systemtime):
        pass
    
    def visit_location(self, timestamp, provider, lat, lon, alt, acc):
        pass
    
    def visit_sensor(self, timestamp, sensor_type, x, y, z):
        pass


class PrintVisitor(DataVisitor):
    def visit_time(self, timestamp, systemtime):
        print('time', timestamp, systemtime)
    
    def visit_location(self, timestamp, provider, lat, lon, alt, acc):
        print("location", timestamp,lat,lon,alt,acc)
    
    def visit_sensor(self, timestamp, sensor_type, x, y, z):
        print("sensor",timestamp, x, y, z)

if __name__ == "__main__":
    file_name = '/tmp/datatest'
    read_data(file_name, PrintVisitor())        
