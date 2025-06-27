#!/bin/sh
set -e

# Configuration – change only if your creds/hosts differ                      #
###############################################################################
AUTH="admin:Zq1!dXr5*Me9"
COOKIE="SAMECOUCHDBCLUSTER"
NODES="couch1 couch2 couch3"
###############################################################################

# Wait until every container answers /_up
echo "⏳  waiting for couchdb nodes …"
for n in $NODES; do
  until curl -fs "http://$n:5984/_up" >/dev/null 2>&1; do sleep 1; done
  echo "  $n is up"
done

# enable_cluster locally on each node
echo "⚙️  1/3 enable_cluster on every node"
for n in $NODES; do
  curl -su "$AUTH" -XPOST "http://$n:5984/_cluster_setup" \
       -H 'Content-Type: application/json' \
       -d '{"action":"enable_cluster",
            "bind_address":"0.0.0.0",
            "username":"admin","password":"'"$AUTH"'",
            "node_count":"3"}' >/dev/null
done

# add couch2 and couch3 to couch1
echo "⚙️  2/3 couch1 adds peers"
for peer in couch2 couch3; do
  curl -su "$AUTH" -XPOST http://couch1:5984/_cluster_setup \
       -H 'Content-Type: application/json' \
       -d '{"action":"add_node",
            "host":"'"$peer"'","port":5984,
            "username":"admin","password":"'"$AUTH"'",
            "remote_current_user":"admin",
            "remote_current_password":"'"$AUTH"'"}' >/dev/null
done

# finish_cluster once (only on the coordinator)
echo "⚙️  3/3 finish_cluster on couch1"
curl -su "$AUTH" -XPOST http://couch1:5984/_cluster_setup \
     -H 'Content-Type: application/json' \
     -d '{"action":"finish_cluster"}' >/dev/null

# wait until couch1 reports cluster_finished
printf "⏳  waiting for cluster_finished … "
while :; do
  state=$(curl -su "$AUTH" http://couch1:5984/_cluster_setup 2>/dev/null |
          grep -o '"state":"[^"]*"' | cut -d':' -f2 | tr -d '"')
  [ "$state" = "cluster_finished" ] && break
  sleep 2
done
echo "done"

echo "✅  CouchDB three-node cluster ready (cookie: $COOKIE)"