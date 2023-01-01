#!/bin/sh

var=`date +"%FORMAT_STRING"`
now=`date +"%Y%m%d-%H:%M:%S"`

curl -s -k https://192.168.0.143/api/RrbzdmmI5N23cBiKEOLHUyayD6yZTrV0azbnKC3b/sensors | python3 -m json.tool | egrep "(name|temperature)" | egrep "(Kitchen|Study|Sara|\"temperature\")" | paste - - | sed -n 's/\.*\"name\": "\(.*\)\",.*\"temperature\": \(.*\),/\1,\2/p' > /home/mark/Documents/hue/temp/temp_${now}.csv

echo ""
