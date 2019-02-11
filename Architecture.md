The architecture divides on 3 parts.

Master node.

Prepare input for nodes, synchronizes layer processing(checks that nodes will not process layer until previous have not done), send information about current status and results to the other neuron webs.
Communicate with nodes via REST.

Worker node.

Run neurons with input and commit result.

File system or database.

Store input and neuron configurations. Workers and mater should have access to  the same file system.


Usage scenario:
Pre requirements: Installed master, workers and file system or db.
User should define configuration of neuronweb:
1. layer amount, amount(at least initial) of neurons for each layer.
2. set up neuron configuration(add signal processors, processors invoke chain, signals).
p.s. for this purpose will be implemented plugin for eclipse or idea  which will scan project for suitable classes and add it as items to configuration editor (some gui tool). This tool will generate configuration.
3. Add file system or database to context.
4. Add studying algorithm to context.(by implementing class or and by adding signal processors which will change neuron or and axon configurations)
Then compile jar and send to master.

