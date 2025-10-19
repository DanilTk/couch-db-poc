### Dataset
https://drive.google.com/uc?export=download&id=1IXQDp8Um3d-o7ysZLxkDyuvFj9gtlxqz


### Import data to couchdb
```bash
docker cp ./init/customers_dataset.csv postgres:/tmp/customers_dataset.csv

docker exec -it postgres psql -U myuser -d mydatabase -c "\
COPY customers (
idx, customer_id, first_name, last_name, company,
city, country, phone1, phone2, email, subscription_date, website
) FROM '/tmp/customers_dataset.csv' WITH (FORMAT csv, HEADER, DELIMITER ',', QUOTE '\"')"
```

Update headers in CSV file to match DB schema
```bash
import pandas as pd

# Define header mapping from original to DB schema
header_map = {
    "Index": "idx",
    "Customer Id": "customer_id",
    "First Name": "first_name",
    "Last Name": "last_name",
    "Company": "company",
    "City": "city",
    "Country": "country",
    "Phone 1": "phone1",
    "Phone 2": "phone2",
    "Email": "email",
    "Subscription Date": "subscription_date",
    "Website": "website"
}

# Input/output paths
input_csv = "customers_dataset.csv"
output_csv = "customers_dataset-renamed.csv"

# Load CSV in chunks to avoid memory issues
reader = pd.read_csv(input_csv, chunksize=100_000)

# Rename headers in first chunk and write to new file
first_chunk = next(reader)
first_chunk.rename(columns=header_map, inplace=True)
first_chunk.to_csv(output_csv, index=False)

# Append remaining chunks
for chunk in reader:
    chunk.to_csv(output_csv, mode="a", header=False, index=False)

print("✅ Headers renamed and saved to:", output_csv)
```


# 🐇 Where the 5-Hour Rabbit-Hole Started … and the Real Blockers We Discovered

## 🧩 Phase Breakdown

| Phase                                               | What We Assumed                                                                           | What Was Actually Happening                                                                                                                   | How It Broke                                                                                                                                |
|-----------------------------------------------------|-------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| **1. Initial Docker Compose**                       | “If I spin up three `couchdb:` containers and point them at each other, they’ll cluster.” | Each container generated its own random Erlang cookie in its data volume.                                                                     | Erlang distribution refused the handshake → every node stayed single → system DBs never replicated → endless `_users does not exist` noise. |
| **2. First Fixes (Common Cookie & _cluster_setup)** | “Same cookie + `/_cluster_setup` will do it.”                                             | We used short node-names (`couch2`) while `couch1` was already in long-name mode (`couchdb@couch1`).                                          | Erlang logged “Hostname couch2 is illegal” and still refused the connection.                                                                |
| **3. Try to Outsmart Docker with `$(hostname)`**    | “Let’s generate the long name inside `ERL_FLAGS` → `-name couchdb@$(hostname)`.”          | Docker never expands `$(hostname)` in env vars → every node booted with the **literal** name `couchdb@$(hostname)`.                           | Erlang detected duplicate node-names → clustering silently failed.                                                                          |
| **4. Cookie OK, Names OK, Still Not Joining**       | “Maybe cluster needs a manual nudge?”                                                     | `init-couch.sh` sent `remote_current_password: "admin:pass"` (username included). `couch2/3` answered 401 → no rows were written to `_nodes`. | Cluster stayed half-configured: `couch1` listed peers; peers listed only themselves.                                                        |
| **5. Side Quests**                                  | Nginx sticky/IP-hash, TestContainers, multiple rewrites of `docker-compose`.              | Not root causes; only obscured the real issues.                                                                                               | 🕳️ Major time sink.                                                                                                                        |

---

## ✅ Key Takeaways

Before calling `/_cluster_setup`, **three conditions must all be true**:

1. **Identical Erlang cookie** on every node  
   _Set via environment variable or copied volume._
2. **Each node has a unique and consistent Erlang node-name**  
   _Use either all long (`couchdb@host.domain`) or all short (`couchdb@host`) names. Never mix._
3. **Every node can resolve all others' names**  
   _Use Docker DNS or `/etc/hosts`._

---

## ⚠️ `_cluster_setup` JSON Warnings

- `password` / `remote_current_password` = just the password (not `user:pass`)
- If the field is wrong, the API returns `401` — but Docker logs often **swallow** the error.

---

## ✅ What Finally Fixed It

- Hard-coded long node-names in `NODENAME`  
  (e.g., `couchdb@couch1.local`, `couchdb@couch2.local`, etc.)
- Single, shared `ERLANG_COOKIE` and `COUCHDB_SECRET`
- Correct `/_cluster_setup` payload with just:

  ```json
  {
    "action": "enable_cluster",
    "bind_address": "0.0.0.0",
    "username": "admin",
    "password": "Zq1!dXr5*Me9",
    "node_count": "3"
  }

-----

## ❓ Why Does the Script Call `enable_cluster` Three Times?

Even though we end up with a **single cluster**, the script calls `enable_cluster` individually on **each node**.

### 🧠 What Each Stage Actually Does

| **Stage in Script**                        | **What the API Call Really Does**                                                                                                                              | **Scope**      | **Result**                                                                        |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------|-----------------------------------------------------------------------------------|
| `enable_cluster` on couch1, couch2, couch3 | Tells that node it is going to participate in a cluster: sets bind-address, stores shared secret, verifies admin user, writes a local flag in `_local/system`. | *Per-node*     | Each node becomes “cluster-ready” — but they’re not yet connected to each other.  |
| `add_node` (run from couch1)               | Inserts `couch2` and `couch3` into the `_nodes` database (a special DB that replicates to all members).                                                        | *Cluster-wide* | All nodes are aware of each other, but system DB replication has not started yet. |
| `finish_cluster` (also from couch1)        | Final validation. Once every node in `_nodes` responds, CouchDB marks the cluster state as `cluster_finished` and begins replicating system DBs.               | *Cluster-wide* | The **single, fully-formed cluster** becomes live.                                |

---

### ✅ So, Why `enable_cluster` on Every Node?

- It’s **required** by CouchDB’s official bootstrap flow.
- It **does not create separate clusters** — it just prepares each node to **join** a cluster.
- Only **one `_nodes` database** exists (once replication starts, it’s shared across all nodes).
- The API is **idempotent**:
    - If you re-run the script, nodes that are already enabled return:

      ```json
      {
        "error": "bad_request",
        "reason": "Cluster is already enabled"
      }
      ```

    - This is harmless and can be safely ignored.

---

## ❓ Could We Have Called `enable_cluster` Only Once?

Yes — you can skip the per-node loop by using the **remote-node** variant of the API. For example:

```json
{
  "action": "enable_cluster",
  "username": "admin",
  "password": "…",
  "bind_address": "0.0.0.0",
  "node_count": "3",
  "remote_node": "couch2.local",
  "remote_current_user": "admin",
  "remote_current_password": "…"
}
```

You would repeat the same request for `couch3.local`.

---

### 🤔 Why We Chose the Local-Call Pattern

We chose the **"local call on every node"** approach because:

- It’s a bit **simpler to read and reason about**.
- It **survives network ACL restrictions**, which might prevent `couch1` from reaching ports `5984`/`5986` (HTTP/Erlang)
  on the other nodes before the Erlang cookies are aligned.

---

### 🧵 Either Approach Works

- Both methods result in a **single, three-node cluster** after `finish_cluster` runs.
- CouchDB internally shares the `_nodes` database, so there is **no risk of creating three separate clusters**.
- Your script merely **prepares three nodes**, then **stitches them together** into one functioning cluster.

----

# ✅ Expected Behavior from CouchDB Cluster + Nginx Setup

Below is the straight-to-the-point behaviour you can expect from the cluster + Nginx setup we just finished.

---

## 🔧 1. What Happens If `couch1` Goes Down?

| **Aspect**                | **Behaviour (with default `?n=3`, `q=2`, `w=2`, `r=2`)**                                                                                            |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Cluster health**        | Cluster stays green as long as **2 of 3 nodes** are up (majority quorum). `_users`, `_replicator`, and your application DBs continue working.       |
| **Reads (`r=2`)**         | Needs 2 replicas. With 1 node down, the other 2 suffice → **reads succeed automatically**.                                                          |
| **Writes (`w=2`)**        | Still possible: 2 replicas required → writes continue, though **a bit slower** since quorum now hits both remaining nodes.                          |
| **Failover in Nginx**     | `max_fails=3`, `fail_timeout=15s` → Nginx marks `couch1` down after 3 failed requests in 15 seconds and **stops routing traffic** to it.            |
| **When `couch1` returns** | On first successful `/` or `/_up` response, Nginx marks it healthy again and resumes routing; CouchDB auto-replicators **backfill missing shards**. |

> ✅ **Summary:** A **single-node failure is survivable** without manual intervention.  
> ❌ **Lose two nodes**, and quorum is gone — the last node runs but **writes (w=2) are rejected**.

---

## 🤔 2. What Exactly Does `ip_hash` Do?

- Nginx hashes each **client IP** once and sticks to that **same CouchDB node** until it's marked down.
- All requests from the same **Spring Boot instance** (same container IP) will hit the **same CouchDB node**.
- A second Spring Boot instance (with a different IP) is hashed separately and likely routed to a different CouchDB
  node.

### In Short:

Spring Boot #1 ─────► couch2   (always, while up)
client IP A
Spring Boot #2 ─────► couch3   (always, while up)
Spring Boot #3 ─────► couch1   (until couch1 marked down)

---

## ⚖️ Do I Still Get Load Balancing?

**Yes**, across **different client IPs**:

- One service container sticks to **one CouchDB node**.
- Add a second replica of the service, and it will likely stick to a **different CouchDB node** — thus distributing
  load.

---

## 🔁 Want Per-Request Load Balancing Instead?

- Remove or replace `ip_hash;` in Nginx.
- By default, Nginx uses **round-robin** (or `random;` in newer versions).
- This spreads **individual requests** across CouchDB nodes, not entire clients.

---

## 🔍 Command to Check CouchDB Cluster Health and Replication

### 📝 Write a Document to a Random Node

```bash
PORTS=(15984 25984 35984); \
PORT=${PORTS[$RANDOM % ${#PORTS[@]}]}; \
ID=$(uuidgen); \
echo "→ writing doc $ID to localhost:$PORT"; \
curl -s -u admin:'Zq1!dXr5*Me9' \
  -H 'Content-Type: application/json' \
  -X PUT "http://localhost:$PORT/test/$ID?w=2" \
  -d '{"msg":"hello"}' | jq .
```

### 📖 Check Document Replication Across All Nodes

```bash
for PORT in 15984 25984 35984; do
  echo -n "localhost:$PORT → "
  curl -s -u admin:'Zq1!dXr5*Me9' \
    http://localhost:$PORT/test | jq -r '.doc_count'
done
```

---

## Discussion Notes for CouchDB Cluster

- ✅ **Use write quorum `w=3`, read quorum `r=1` in LightCouch**
  ```java
  JsonObject doc = new JsonObject();
  doc.addProperty("_id", UUID.randomUUID().toString());
  doc.addProperty("msg", "hello");

  Params params = new Params()
      .addParam("w", "3")   // wait until all 3 nodes have fsynced
      .addParam("r", "1");  // require 1 replica on reads

  client.save(doc, params);  // POST /db/... ?w=3&r=1
  ```
- **Balance read quorum (`r`) against write quorum (`w`)**
    - `r=1` offers low latency but may allow stale reads.
    - `r=2` improves consistency but adds latency.

- **📦 Determine number of Spring Boot instances**
    - Affects load balancing, connection pool sizing, and CouchDB concurrency.

- **🔀 Decide whether to keep `ip_hash` in Nginx**
    - Sticky sessions ensure “read-your-write” behavior.
    - Round-robin improves load distribution but may cause stale reads.

- **🧹 Schedule periodic compaction**
    - Clears tombstones and old document revisions.
    - Should run during off-peak traffic windows.

--- 
Check cluster status:

```bash
for p in 15984 25984 35984; do
  curl -s -u 'admin:Zq1!dXr5*Me9' "http://localhost:$p/_membership" | jq .
done
```

curl -u admin:'Zq1!dXr5*Me9' \
-H "Content-Type: application/json" \
-X POST http://localhost:5984/_replicator \
-d '{
"_id"        : "rep-saint-martin",
"source"     : "http://admin:Zq1!dXr5*Me9@localhost:5984/customers",
"target"     : "http://admin:Zq1!dXr5*Me9@localhost:5984/customers_00d7cbfb",
"selector"   : { "country": "Saint Martin" },
"continuous" : true,
"create_target": false
}'