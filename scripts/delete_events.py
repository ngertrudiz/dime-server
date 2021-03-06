#!/usr/bin/env python3

import requests
import sys
import socket
import time
import json
import dime

#------------------------------------------------------------------------------

class DiMeConnection:
    def __init__(self, url, username, uid=None):
        self.url = url
        self.username = username
        self.uid = uid
        self.password = dime.password(self.username)

    def ping(self, verbose=True):
        r = requests.post(self.url + '/ping')

        if r.status_code != requests.codes.ok:
            if verbose:
                print('No connection to DiMe server!', file=sys.stderr)
            return None
        else:
            if verbose:
                print('Connected to DiMe: {} @ {}, version {}.'.
                      format(self.username, self.url, r.json()['version']),
                      file=sys.stderr)
            return r.json()

    def post(self, url):
        print("POST", url)
        r = requests.post(self.url + url, headers={'content-type': 'application/json'},
                          auth=(self.username, self.password), timeout=10)
        return r

    def get(self, url):
        print("GET", url)
        r = requests.get(self.url + url, headers={'content-type': 'application/json'},
                          auth=(self.username, self.password), timeout=10)
        return r

    def delete(self, url):
        print("DELETE", url)
        r = requests.delete(self.url + url, headers={'content-type': 'application/json'},
                            auth=(self.username, self.password), timeout=10)
        return r
    
#------------------------------------------------------------------------------

dime = DiMeConnection('http://localhost:8081/api', 'demouser')

if not dime.ping():
    sys.exit(1)

r = dime.get('/data/events?' + sys.argv[1])

rj = r.json()

if len(rj) == 0:
    print('No events found.')
    sys.exit(1)

print('These would be deleted:', json.dumps(rj, indent=2))

input('Press enter to continue (or Ctrl-C to cancel)')


for item in rj:
    item_id = item['id']
    r = dime.delete('/data/event/' + str(item_id))

    if r.status_code == requests.codes.ok:
        print('Successfully deleted event', item_id)
    else:
        print('Error deleting event', item_id, r)
