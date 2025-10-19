#!/bin/sh
set -e

NODES="couch1.local couch2.local couch3.local"

echo "⏳ waiting for nodes …"
for n in $NODES; do
  until curl -fs "http://$n:5984/_up" >/dev/null; do sleep 1; done
  echo "  $n up"
done

echo "⚙ enable_cluster on each node"
for n in $NODES; do
  curl -su admin:Zq1!dXr5*Me9 -XPOST http://$n:5984/_cluster_setup \
       -H 'Content-Type: application/json' \
       -d '{"action":"enable_cluster","bind_address":"0.0.0.0",
            "username":"admin","password":"Zq1!dXr5*Me9","node_count":"3"}'
done

echo "⚙ couch1 adds couch2 & couch3"
for peer in couch2.local couch3.local; do
  curl -su admin:Zq1!dXr5*Me9 -XPOST http://couch1.local:5984/_cluster_setup \
       -H 'Content-Type: application/json' \
       -d '{"action":"add_node",
            "host":"'"$peer"'","port":5984,
            "username":"admin","password":"Zq1!dXr5*Me9",
            "remote_current_user":"admin",
            "remote_current_password":"Zq1!dXr5*Me9"}'
done

echo "⚙ finish_cluster on couch1"
curl -su admin:Zq1!dXr5*Me9 -XPOST http://couch1.local:5984/_cluster_setup \
     -H 'Content-Type: application/json' \
     -d '{"action":"finish_cluster"}'

echo "✅ cluster_finished"