#!/bin/bash
#SBATCH --job-name=jpansim2
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=144
#SBATCH --mem=200G
#SBATCH --time=06:00:00
#SBATCH --output=slurm-%A_%a.out
#SBATCH --error=slurm-%A_%a.err


## Load appropriate module
## module purge
## module load openjdk/17-arm

## Set JVM options, leaving 50g for non heap.
export JAVA_OPTS="-Xms4g -Xmx150g"

## Uncomment to enable profiling

## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.port=5555"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.rmi.port=5556"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.authenticate=false"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.ssl=false"
## export JAVA_OPTS+=" -Djava.rmi.server.hostname=localhost"

## The connection to the remote machine is going to be difficult
##  ssh -D 1337 -q -C -N user.project@nodename
## The configure visualVM to use the SOCKS proxy (using port 1337)
## This is under tools > options > network > manual proxy settings
## Then connect add JMX connection and use localhost:9999

## Launch with correct CPU binding
srun java $JAVA_OPTS -jar ~/bin/jpansim2-0.1.3-jar-with-dependencies.jar
