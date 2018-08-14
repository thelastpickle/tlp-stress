---
title: tlp-stress
author: Jon Haddad
toc-title: Table of Contents

---

# Introduction

tlp-stress is a workload-centric stress tool, written in Kotlin.  Workloads are easy to write and because they are written in code, you have the ultimate flexibility.  Workloads can be tweaked via command line parameters to make them fit your environment more closely.

One of the goals of tlp-stress is to provide enough pre-designed workloads *out of the box* so it's unnecessary to code up a workload for most use cases.  For instance, it's very common to have a key value workload, and want to test that.  tlp-stress allows you to customize a pre-configured key-value workload, using simple parameters to modify the workload to fit your needs.  Several workloads are included, such as:

* Time Series
* Key / Value 
* Materialized Views
* Collections (maps)
* Counters

The tool is flexible enough to design workloads which leverage multiple (thousands) of tables, hitting them as needed.  Statistics are automatically captured by the Dropwizard metrics library.

# Quickstart Example

Assuming you have either a CCM cluster or are running a single node locally, you can run this quickstart.

Either add the `bin` directory to your PATH or from within tlp-stress run:


    bin/tlp-stress run KeyValue -i 1M -p 100k
    


# Building the Stress Tool

How to build 

    gradle assemble
    
# Building the documentation

There's a custom gradle task to build the manual, pandoc must be installed:
    
    docker-compose run pandoc

# Usage

## Listing All Workloads

## Getting infomration about a workload

* Description
* Schema


## Running a Stress Workload

Once you've built the application, you can run a stress workload.  

Run

    bin/tlp-stress -h 
    
to see all the help options.



### Customzing Fields

To some extent, workloads can be customized by leveraging the `--fields` flag.  For instance, if we look at the KeyValue workload, we have a table called `keyvalue` which has a `value` field.

To customize the data we use for this field, we provide a generator at the command line.  By default, the `value` field will use 100-200 characters of random text.  What if we're storing blobs of text instead?  Ideally we'd like to tweak this workload to be closer to our production use case.  Let's use random sections from various books:

    bin/tlp-stress run KeyValue --field.keyvalue.value='book(20,40)`
    
Instead of using random strings of garbage, the KeyValue workload will now use 20-40 words extracted from books.

There are other generators available, such as names, gaussian numbers, and cities.  Not every generator applies to every type.  It's up to the workload to specify which fields can be used this way.
