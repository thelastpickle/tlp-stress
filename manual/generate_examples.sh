#!/usr/bin/env bash

print_shell() {
    # params
    # $1 = name
    # #2 = command

    echo "running $1"

    printf "$ %s\n" "$2" > manual/examples/"${1}.txt"
    $2 >> manual/examples/"${1}.txt"
}


./gradlew assemble

# help
print_shell "tlp-stress-help" "bin/tlp-stress"

# key value
print_shell "tlp-stress-keyvalue" "bin/tlp-stress run KeyValue -n 10000"

# info
print_shell "info-key-value" "bin/tlp-stress info KeyValue"


# list all workloads
print_shell "list-all" "bin/tlp-stress list"

print_shell "field-example-book" "bin/tlp-stress run KeyValue --field.keyvalue.value='book(20,40)"



