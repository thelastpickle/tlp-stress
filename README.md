# tlp-stress: A workload centric stress tool and framework

This project is a work in progress. 

cassandra-stress is a configuration-based tool for doing benchmarks and testing simple datamodels for Apache Cassandra.  Unfortunately it can be difficult to configure a workload.  There are faily common data models and workloads seen on Apache Cassandra, this tool aims to provide a means of executing configurable, pre-defined profiles.

In addition to creating workloads, tlp-stress can also (maybe?) verify the data in the DB is what should be visible.  The amount of data to verify can be configured via a sampling rate.  By default 1% of mutations are sampled and retained in memory for later verification.


This tool is in its infancy and not ready for general usage.


# Examples

Time series workload with a billion operations:

    bin/tlp-stress run BasicTimeSeries -i 1B

Key value workload with a million operations across 5k partitions, 50:50 read:write ratio:

    bin/tlp-stress run KeyValue -i 1M -p 5k -r .5



