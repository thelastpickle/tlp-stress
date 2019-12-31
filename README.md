# tlp-stress: A workload centric stress tool and framework

This project is a work in progress.

[![CircleCI](https://circleci.com/gh/thelastpickle/tlp-stress.svg?style=svg)](https://circleci.com/gh/thelastpickle/tlp-stress)

Please see our [Google Group](https://groups.google.com/d/forum/tlp-dev-tools) for discussion.

cassandra-stress is a configuration-based tool for doing benchmarks and testing simple datamodels for Apache Cassandra.  Unfortunately it can be difficult to configure a workload.  There are faily common data models and workloads seen on Apache Cassandra, this tool aims to provide a means of executing configurable, pre-defined profiles.

Full docs are here: http://thelastpickle.com/tlp-stress/

# Installation

The easiest way to get started on Linux is to use system packages.  Instructions for installation can be found here: http://thelastpickle.com/tlp-stress/#_installation


# Building

Clone this repo, then build with gradle:

    git clone https://github.com/thelastpickle/tlp-stress.git
    cd tlp-stress
    ./gradlew shadowJar

Use the shell script wrapper to start and get help:

    bin/tlp-stress -h

# Examples

Time series workload with a billion operations:

    bin/tlp-stress run BasicTimeSeries -i 1B

Key value workload with a million operations across 5k partitions, 50:50 read:write ratio:

    bin/tlp-stress run KeyValue -i 1M -p 5k -r .5


Time series workload, using TWCS:

    bin/tlp-stress run BasicTimeSeries -i 10M --compaction "{'class':'TimeWindowCompactionStrategy', 'compaction_window_size': 1, 'compaction_window_unit': 'DAYS'}"

Time series workload with a run lasting 1h and 30mins:

    bin/tlp-stress run BasicTimeSeries -d "1h30m"

Time series workload with Cassandra Authentication enabled:

    bin/tlp-stress run BasicTimeSeries -d '30m' -U '<username>' -P '<password>'
    **Note**: The quotes are mandatory around the username/password
    if they contain special chararacters, which is pretty common for password
