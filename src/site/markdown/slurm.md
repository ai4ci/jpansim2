

```sh
#!/bin/bash
#SBATCH --job-name=jpansim2
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=72
#SBATCH --mem=115G
#SBATCH --time=06:00:00
#SBATCH --output=slurm-%A_%a.out
#SBATCH --error=slurm-%A_%a.err

# Map the 0-3 array ID to a 0-1 Socket ID
# (0 and 2 -> Socket 0) | (1 and 3 -> Socket 1)
SOCKET_ID=$(( SLURM_ARRAY_TASK_ID % 2 ))

## For a multithreaded app it is best to keep to the 72 codes of a single grace CPU (2 per node)
## running with the memory limits.
## This script can be further parallelised by specifying --array=0-1 to use 1 node or --array=0-3 for 2

## Load appropriate module
## module purge
## module load openjdk/17-arm
## N.b. Isambard does not have openjdk modules default java is version 11.

## Set JVM options, leaving 20g for non heap.
export JAVA_OPTS="-Xms4g -Xmx100g"

COMPUTE_HOSTNAME=$(hostname)

## Profiling:

## Create a script that tunnels jmxremote from local to the compute node
## This will be created by the compute node but needs to be copied to and run
## on the local machine.
cat << EOF > tunnel.sh
#!/bin/bash
ssh -N \
    -o BatchMode=yes \
    -o ServerAliveInterval=10 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -L 5555:$COMPUTE_HOSTNAME:5555 \
    -L 5556:$COMPUTE_HOSTNAME:5556 \
    $USER@b35ar.3.isambard

visualvm --openjmx localhost:5555
EOF

export JAVA_OPTS+=" -Dcom.sun.management.jmxremote"
export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.port=5555"
export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.rmi.port=5556"
export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.authenticate=false"
export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.ssl=false"
export JAVA_OPTS+=" -Djava.rmi.server.hostname=localhost"

export JAVA_OPTS+=" -XX:ReservedCodeCacheSize=128M -XX:InitialCodeCacheSize=128M"
export JAVA_OPTS+=" -XX:+UseParallelGC"  # Better for high-core count throughput than G1

## Launch with correct CPU binding
# srun numactl --cpunodebind=$SOCKET_ID --membind=$SOCKET_ID \
srun --cpu-bind=cores \
    java $JAVA_OPTS -jar $PROJECTDIR/bin/jpansim2-core-0.3.4-jar-with-dependencies.jar

```
