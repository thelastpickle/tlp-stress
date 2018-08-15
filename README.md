# tlp-stress: A workload centric stress tool and framework

This project is a work in progress. 

cassandra-stress is a configuration-based tool for doing benchmarks and testing simple datamodels for Apache Cassandra.  Unfortunately it can be difficult to configure a workload.  There are faily common data models and workloads seen on Apache Cassandra, this tool aims to provide a means of executing configurable, pre-defined profiles.

Full docs are here (sort of, work in progress): http://thelastpickle.com/tlp-stress/

# Building

Clone this repo, then build with gradle:

    git clone https://github.com/thelastpickle/tlp-stress.git
    cd tlp-stress
    ./gradlew assemble
    
Use the shell script wrapper to start and get help:

    bin/tlp-stress -h

# Examples

Time series workload with a billion operations:

    bin/tlp-stress run BasicTimeSeries -i 1B

Key value workload with a million operations across 5k partitions, 50:50 read:write ratio:

    bin/tlp-stress run KeyValue -i 1M -p 5k -r .5


Time series workload, using TWCS:

    bin/tlp-stress run KeyValue -i 10M --compaction "{'class':'TimeWindowCompactionStrategy', 'compaction_window_size': 1, 'compaction_window_unit': 'DAYS'}"

