#!/bin/bash
#SBATCH --job-name=jpansim2
#SBATCH --nodes=1
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=16
#SBATCH --mem=115G
#SBATCH --sockets-per-node=1
#SBATCH --time=06:00:00
#SBATCH --output=slurm_%a.out
#SBATCH --error=slurm_%a.err
#SBATCH --signal=B:TERM@60
#SBATCH --mem-bind=local

## signal soft terminates the app before killing it when scancel is called. Allows for clean up of files.

## For a multithreaded app it is best (?) to keep to the 72 codes of a single grace CPU (2 per node)
## running with the memory limits.
## This script can be further parallelised by specifying --array=1-2 to use 1 node or --array=1-4 for 2

## N.b. Isambard does not have openjdk modules default java is version 11.
## graalvm 25 installed on PROJECTDIR
export PATH=$PROJECTDIR/graalvm/bin:$PATH
export JAVA_HOME=$PROJECTDIR/graalvm
export JAVA_OPTS=""

## Set JVM options, leaving 20g for non heap.
## export JAVA_OPTS="-Xms4g -Xmx100g"
export JAVA_OPTS+=" -XX:InitialHeapSize=64G -XX:MinHeapSize=64G -XX:MaxHeapSize=64G -XX:+AlwaysPreTouch"
## export JAVA_OPTS+=" -XX:+UseG1GC -XX:+UseLargePages -XX:ThreadStackSize=1024"
export JAVA_OPTS+=" -XX:ReservedCodeCacheSize=128M -XX:InitialCodeCacheSize=128M"
export JAVA_OPTS+=" -XX:ActiveProcessorCount=$SLURM_CPUS_PER_TASK"
## export JAVA_OPTS+=" -XX:+UseNUMA"


## debug code cache issues:
export JAVA_OPTS+=" -XX:+PrintCodeCache"


## Profiling:


COMPUTE_HOSTNAME=$(hostname)
PROJECT_ID=$(basename "$PROJECTDIR")

## Creates a script that tunnels jmxremote from local to the compute node
## This will be created by the compute node but needs to be copied to and run
## on the local machine.
cat << EOF > tunnel.sh
#!/bin/bash

# 1. Start SSH in the background using the submission host variable
ssh -fN \
    -o BatchMode=yes \
    -o ServerAliveInterval=10 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -L 5555:$COMPUTE_HOSTNAME:5555 \
    -L 5556:$COMPUTE_HOSTNAME:5556 \
    $USER@$PROJECT_ID.3.isambard

# Get the PID of the last background process
SSH_PID=\$!

# 2. Cleanup on exit
trap "kill \$SSH_PID 2>/dev/null" EXIT

# 3. Launch monitoring tool
# visualvm --openjmx localhost:5555
jmc "service:jmx:rmi:///jndi/rmi://localhost:5555/jmxrmi"
EOF

chmod a+x ./tunnel.sh

## uncomment for jmxremote:
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.port=5555"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.rmi.port=5556"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.authenticate=false"
## export JAVA_OPTS+=" -Dcom.sun.management.jmxremote.ssl=false"
## export JAVA_OPTS+=" -Djava.rmi.server.hostname=localhost"

export JAVA_OPTS+=" -XX:StartFlightRecording=name=background,maxsize=100m,filename=recording_${SLURM_ARRAY_TASK_ID}.jfr,dumponexit=true"

## Launch with correct CPU binding
srun --cpu-bind=cores java $JAVA_OPTS -jar $PROJECTDIR/bin/jpansim2-core-0.3.6-jar-with-dependencies.jar

## graalvm native-image:
##    $PROJECTDIR/bin/jpansim2-0.3.5
