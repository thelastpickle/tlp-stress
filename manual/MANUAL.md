---
title: tlp-stress
author: Jon Haddad
toc-title: Table of Contents

---

# Introduction

tlp-stress is a workload-centric stress tool, written in Kotlin.  Workloads are easy to write and because they are based in code, you have the ultimate flexibility.

One of the goals of tlp-stress is to provide enough pre-designed workloads *out of the box* so it's unnecessary to code up a workload for most use cases.  For instance, it's very common to have a key value workload, and want to test that.  tlp-stress allows you to customize a pre-configured key-value workload, using simple parameters to modify the workload to fit your needs.  Several workloads are included, such as

* Time Series
* Key / Value 
* Materialized Views
* Collections (maps)

The tool is flexible enough to design workloads which leverage multiple (thousands) of tables, hitting them as needed.  Statistics are automatically captured by CodaHale's metrics library.

# Quickstart Example

Assuming you have either a CCM cluster or are running a single node locally, you can run this quickstart.

Either add the `bin` directory to your PATH or from within tlp-stress run:


    bin/tlp-stress run KeyValue -i 1M -p 100k
    


# Building the Stress Tool

How to build 

    gradle assemble
    
# Building the documentation

There's a custom gradle task to build the manual, pandoc must be installed:
    
    gradle compileManual

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



# Creating a Workload

