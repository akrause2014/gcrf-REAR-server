'''
export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:/usr/local/mysql-5.6.20-osx10.8-x86_64/lib
'''
import os
import datetime
import numpy as np

import MySQLdb
from obspy.core.trace import Trace
from obspy.core.stream import Stream
from obspy.core.utcdatetime import UTCDateTime

from read_rear import DataVisitor, read_data

# DATA_DIR = '/tmp/data/'
DATA_DIR = '/Users/akrause/gcrf-REAR/data/mobile/'

def to_datetime(timestamp):
    return datetime.datetime.fromtimestamp(timestamp)

class TraceVisitor(DataVisitor):
    
    def __init__(self):
        self.timezero = None
        self.start_elapsed = None
        self.end_elapsed = None
        self.buffer_x = []
        self.buffer_y = []
        self.buffer_z = []
        self.timestamps = []
        self.delta = []
        self.location = None
        
    def get_traces(self):
        delta = round((sum(self.delta) / float(len(self.delta)))/1000, 2)
#         print("delta = %s" % delta)
#         print("timezero = %s" % self.timezero)
#         print("start elapsed = %s" %int(self.start_elapsed/1000000))
#         print("start time = %s" % (self.timezero+int(self.start_elapsed/1000000)))
#         print("end time = %s" % (self.timezero+int(self.end_elapsed/1000000)))
        stats = {
                    'delta': delta, 
                    'starttime': UTCDateTime((self.timezero+self.start_elapsed/1000000)/1000), 
                    'endtime': UTCDateTime((self.timezero+self.end_elapsed/1000000)/1000),
                }
        if self.location:
            stats['coordinates'] = {
                        'latitude': self.location[1],
                        'longitude': self.location[2],
                        'altitude': self.location[3],
                        'accuracy': self.location[4]
                    }
        print(stats)
        tr_x = Trace(data=np.array(self.buffer_x), header=stats)
        tr_y = Trace(data=np.array(self.buffer_y), header=stats)
        tr_z = Trace(data=np.array(self.buffer_z), header=stats)
        return tr_x, tr_y, tr_z

    def visit_time(self, timestamp, systemtime):
#         print("Time", timestamp, systemtime)
        # timestamp is elapsed time in milliseconds
        # systemtime is in milliseconds too
        self.timezero = systemtime-timestamp
        print(self.timezero)
        pass
    
    def visit_location(self, timestamp, provider, lat, lon, alt, acc):
#         print("Location", timestamp, provider, lat, lon, alt, acc)
        if self.location is None or self.location[4] > acc:
            self.location = provider, lat, lon, alt, acc
    
    def visit_sensor(self, timestamp, sensor_type, x, y, z):
#         print("Sensor", timestamp, sensor_type, x, y, z)
        if self.start_elapsed is None or self.start_elapsed > timestamp:
            self.start_elapsed = timestamp
        if self.end_elapsed is None or self.end_elapsed < timestamp:
            self.end_elapsed = timestamp
        self.buffer_x.append(x)
        self.buffer_y.append(y)
        self.buffer_z.append(z)
        try:
#             print((timestamp-self.timestamps[-1])/1000.0) # delta in milliseconds
            self.delta.append((timestamp-self.timestamps[-1])/1000000.0)
        except:
            pass
        self.timestamps.append(timestamp)

def get_query(device, startdate, enddate):
    return "SELECT id, system, elapsed FROM uploads WHERE device=%s AND system>=%s AND system<=%s" % (device, startdate, enddate)


def read_bin_data(device, startdate, enddate, output_file):
    # connect to database and retrieve upload ids for startdate to enddate
    conn = MySQLdb.connect (host = "localhost",
                            user = "root",
                            passwd = "",
                            db = "rear_meta")
    cursor = conn.cursor()
    length = enddate - startdate
    # select every file between startdate and enddate
    query = get_query(device, startdate, enddate)
    cursor.execute(query)
    st_x = None
    st_y = None
    st_z = None
    print("Number of rows: %s" % cursor.rowcount)
    filecount = 0
    while True:
        row = cursor.fetchone()
        if row is None:
            break
        file_id = row[0]
        # read binary data from data file
        file_name = os.path.join(DATA_DIR, '%s/%s' % (device, file_id))
        print("Reading %s: %s" % (file_id, file_name))
        tv = TraceVisitor()
        read_data(file_name, tv)
        # create obspy stream from data files
        tr_x, tr_y, tr_z = tv.get_traces()
#         print(tr_x.stats)
#         print(tr_y.stats)
#         print(tr_z.stats)
        if st_x is None:
            st_x = tr_x
            st_y = tr_y
            st_z = tr_z
        else:
            st_x = st_x + tr_x
            st_y = st_y + tr_y
            st_z = st_z + tr_z
        filecount += 1

    cursor.close()
    conn.close()

    if st_x is not None:
        st_x.data= st_x.data.filled()
        st_y.data= st_y.data.filled()
        st_z.data= st_z.data.filled()

        st_x.write(output_file + "_x.sac", format='SAC')
        st_y.write(output_file + "_y.sac", format='SAC')
        st_z.write(output_file + "_z.sac", format='SAC')
        
        print("Output X:\n%s" % st_x.stats)
        
    print("Merged %s file(s)" % filecount)

if __name__ == "__main__":
#     device = 17
#     startdate = 1486558441850
#     enddate = 1486558683215
#     output_file = "/tmp/test"

    import argparse

    parser = argparse.ArgumentParser(description='Read binary files in REAR format, merge each channel into a single trace and write out as a SAC file.')
    parser.add_argument('-s', '--startdate', type=int, required=True, help='the start date as a Unix timestamp')
    parser.add_argument('-e', '--enddate', type=int, required=True, help='the end date as a Unix timestamp')
    parser.add_argument('-d', '--device', type=int, required=True, help='the device ID')
    parser.add_argument('-o', '--output', required=True, help='output file name')
    parser.add_argument('-q', '--query', action='store_true', help='show query only')
    args = parser.parse_args()
    
    startdate = args.startdate
    enddate = args.enddate
    device = args.device
    output_file = args.output
    
    if args.query:
        print(get_query(device, startdate, enddate))

    else:
        read_bin_data(device, startdate, enddate, output_file)

