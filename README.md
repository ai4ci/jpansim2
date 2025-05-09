# jpansim2

JPanSim2 is an agent based infectious disease outbreak simulation. This is a 
discrete time stochastic ABM. Disease transmission is simulated on a social
network with links weighted by closeness of the relationship. Contacts within
the social network are simulated depending on agent's mobility levels.
Once a contact is made disease may transmit between individuals depending on a 
pairwise and individual transmission probability, with successful transmission
resulting in an exposure to an individual. Each agent has an in-host viral load
and immunity model. After exposure, in-host viral replication and immune activation
is simulated and this determines onwards infectivity, symptoms, and test results.
Each agent has a behaviour state model, in which relevant behaviour is modelled
to respond to test results, symptoms, or perceived risk of infection. Various state
models are implemented, for different behaviours. Behaviour states are associated
with action recommendations which may or may not be followed by any individual 
depending on their individual level of compliance. At a system level a policy 
state model is implemented to represent top down non pharmaceutical interventions
such as lock downs.

Throughout the simulation each agents behaviour in terms of mobility, transmissibility,
or compliance to guidance will alter depending on prior history and behavioural 
model. Likewise test seeking behaviour can be triggered by local and system wide 
events. 

The simulation requires a java 11 runtime environment. It is currently targetted
at command line usage. It can be invoked with the following command:

```
java -jar target/jpansim2-0.1.1-jar-with-dependencies.jar -o ~/tmp
```

Where valid command line options are as follows:

```
JPanSim2 command line options.
 -c,--config <config>       The path to the configuration file. Defaults
                            to "config.json" in the output directory.
 -o,--output <output>       The path to the output directory. Defaults to
                            the current working directory.
N.B. Slurm support: batch commands must be continuous and start at 1
e.g. sbatch --array=1-32
```

The simulation expects a json configuration file, with path supplied as a parameter or
called `config.json` and located in the output directory (`~/tmp` in the
 example above). 

The configuration files allow for multiple simulations to be configured, by 
defining a base configuration and specifying "facets" containing a list
of modifications to the base configuration. Each combination of modification
within all facets is run to allow for a comparison grid. Simulations are 
typically memory bound so each simulation is run in a multi-threaded manner and 
individual simulations are run sequentially. The set of simulations defined in 
a configuration file each have a distinct identifier (as a URN) and their output
is collected in a set of CSV files in the output directory.

Each simulation is built in two stages, firstly building the simulation environment
including the agents, their demographic features and their social network. One configuration
file allows multiple environment "setup configuration"s, and each setup can be
subject to modification using facets, furthermore each modified setup can be
replicated to allow for the effect of randomness. This can result in many 
environments in a given experiment. Each environment is constructed once and re-used
for each simulation execution. The setup of environments can be parallelised 
across SLURM nodes, in which case the outputs of each node will be in a numbered
sub-directory in the output directory.

Each environment then has a set of simulations executed on it. Each execution
may have different outbreak parameters or population behaviour configured (as
facets). Each simulation execution is run one after another on a single SLURM 
node, but uses multi-threading to execute each simulation as quickly as possible
in a multi-core node. As simulations tend to be memory bound there is no benefit 
simultaneously performing multiple executions.

Example configuration files are found in the `src/test/resources` directory
along with a json schema. Documentation of supported options is in the javadoc for the 
`io.github.ai4ci.config` package.