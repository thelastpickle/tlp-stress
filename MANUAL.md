---
title: tlp-stress
author: Jon Haddad
toc-title: Table of Contents

---

# Building


## Building the Stress Tool

How to build 

    gradle assemble
    
## Building the documentation

There's a custom gradle task to build the manual, pandoc must be installed:
    
    gradle compileManual

# Running

Once you've built the application, you can run a stress workload.  

Run

    bin/tlp-stress -h 
    
to see all the help options.

## Quickstart Example

Assuming you have either a CCM cluster or are running a single node locally, you can run this quickstart.

Either add the `bin` directory to your PATH or from within tlp-stress run:


    bin/tlp-stress run KeyValue -i 1M -p 100k
    
This command will run




