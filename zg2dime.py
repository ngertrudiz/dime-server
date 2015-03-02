#!/usr/bin/env python

import sys
import socket
import requests
import json
import hashlib

from gi.repository import GLib

from zeitgeist.client import ZeitgeistClient
from zeitgeist.datamodel import *
 
def send_event(event):
    payload = {'origin':                 hostname,
               'actor':                  event.actor, 
               'interpretation':         event.interpretation,
               'manifestation':          event.manifestation,               
               'timestamp':              event.timestamp,
               'subject_uri':            event.subjects[0].uri,
               'subject_interpretation': event.subjects[0].interpretation,
               'subject_manifestation':  event.subjects[0].manifestation,
               'subject_mimetype':       event.subjects[0].mimetype}

    headers = {'content-type': 'application/json'}
 
    json_payload = json.dumps(payload)
    md5 = hashlib.md5()
    md5.update(json_payload)
    md5digest = md5.hexdigest()
    payload['id'] = md5digest
    json_payload = json.dumps(payload)
    print(json_payload)

    r = requests.post(server_url, data=json_payload, headers=headers)
    print(r.text)

def on_insert(time_range, events):
    send_event(events[0])

def on_events_received(events):
    for event in events:
        send_event(event)

def on_delete(time_range, event_ids):
    print event_ids

def main():
    GLib.MainLoop().run()

zeitgeist = ZeitgeistClient()
 
template = Event.new_for_values(subject_interpretation=Interpretation.DOCUMENT)

zeitgeist.find_events_for_template(template, on_events_received, num_events=1)
zeitgeist.install_monitor(TimeRange.always(), [template], on_insert, on_delete)

hostname = socket.gethostbyaddr(socket.gethostname())[0]

server_url = 'http://localhost:8080/logger'

if len(sys.argv)>0 and sys.argv[-1] == 'debug':
    server_url = 'http://httpbin.org/post'

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("Exiting")

