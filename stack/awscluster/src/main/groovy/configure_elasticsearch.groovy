/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */



// 
// configure_elasticsearch.groovy 
// 
// Emits Elasticsearch config file based on environment and Elasticsearch node 
// registry in SimpleDB
//


String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
def clusterName  = (String)System.getenv().get("ES_CLUSTER_NAME")

int esNumServers = ((String)System.getenv().get("ES_NUM_SERVERS")).toInteger()
int quorum = esNumServers/2+1;

NodeRegistry registry = new NodeRegistry();

// build seed list by listing all Elasticsearch nodes found in SimpleDB domain with our stackName
def selectResult = registry.searchNode('elasticsearch')
def esnodes = ""
def sep = ""
for (hostname in selectResult) {
   esnodes = "${esnodes}${sep}\"${hostname}\""
   sep = ","
}

def elasticSearchConfig = """
cluster.name: ${clusterName}
discovery.zen.minimum_master_nodes: ${quorum}
discovery.zen.ping.multicast.enabled: false
discovery.zen.ping.unicast.hosts: [${esnodes}]
node:
    name: ${hostName} 
network:
    host: ${hostName}
path:
    logs: /mnt/log/elasticsearch
    data: /mnt/data/elasticsearch

#Set the logging level to INFO by default
es.logger.level: INFO

#Set our threadpool size.  Our bulk pool and search pools are quite large.  We may want to turn these down if we
#overload the system
#
# Temporarily removing.  We don't know better :)
# http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_don_8217_t_touch_these_settings.html#_threadpools
#
#threadpool:
#    index:
#        type: fixed
#        size: 160
#        queue_size: 0
#    bulk:
#        type: fixed
#        size: 5000
#        size: 16
#        queue_size: 100
#    search:
#        size: 10000
#        size: 48
#        type: fixed
#        queue_size: 100

action.auto_create_index: false

action.disable_delete_all_indices: true

#################################
# Operational settings taken from a loggly blog here.  Tweak and work as required
# https://www.loggly.com/blog/nine-tips-configuring-elasticsearch-for-high-performance/
#################################

#Set the mlock all to better utilize system resources
bootstrap.mlockall: true

#Only cache 25% of our available memory
indices.fielddata.cache.size: 25%

#If you haven't used it in 10 minutes, evict it from the cache
#indices.fielddata.cache.expire: 10m

#Only allow rebalancing of 2 shards at a time
cluster.routing.allocation.cluster_concurrent_rebalance: 2

#Re-shard when our disks start getting full
cluster.routing.allocation.disk.threshold_enabled: true
cluster.routing.allocation.disk.watermark.low: .97
cluster.routing.allocation.disk.watermark.high: .99

#Set streaming high water marks so reboots don't kill our service
cluster.routing.allocation.node_concurrent_recoveries: 4
cluster.routing.allocation.node_initial_primaries_recoveries: 18
indices.recovery.concurrent_streams: 4
indices.recovery.max_bytes_per_sec: 40mb


###############
# Logging options
# We want to turn on logging for slow queries and executions, so
###############

index.search.slowlog.threshold.query.warn: 10s
index.search.slowlog.threshold.query.info: 5s
index.search.slowlog.threshold.query.debug: 2s
index.search.slowlog.threshold.query.trace: 500ms

index.search.slowlog.threshold.fetch.warn: 1s
index.search.slowlog.threshold.fetch.info: 800ms
index.search.slowlog.threshold.fetch.debug: 500ms
index.search.slowlog.threshold.fetch.trace: 200ms


index.indexing.slowlog.threshold.index.warn: 10s
index.indexing.slowlog.threshold.index.info: 5s
index.indexing.slowlog.threshold.index.debug: 2s
index.indexing.slowlog.threshold.index.trace: 500ms


"""

println elasticSearchConfig
