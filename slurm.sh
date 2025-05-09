#!/bin/bash
#SBATCH --job-name=jpansim2
##SBATCH --array=1-10
#SBATCH --array=1
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=128
#SBATCH --mem=240G
#SBATCH --time=06:00:00
#SBATCH --output=slurm-%A_%a.out
#SBATCH --error=slurm-%A_%a.err
#SBATCH --partition=standard



## Load appropriate module
## module purge
## module load openjdk/17-arm

## Set JVM options, leaving 60g for non heap.
export JAVA_OPTS="-Xms4g -Xmx180g"

## Uncomment to enable profiling
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.port=5555"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.rmi.port=5556"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.authenticate=false"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.ssl=false"
## export JAVA_OPTS+=" -Djava.rmi.server.hostname=localhost"


## Launch with correct CPU binding
srun --cpu-bind=map_cpu:0-127 java $JAVA_OPTS -jar application.jar
