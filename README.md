#Purpose:
Maine purpose of this framework is allow AI developers to build object oriented model of brain.
Additional purpose is create platform which will allow to implement neuron net with any known design  
and run it localy  or in distributed way on cluster, cuda cluster, aws lambdas.

#Concepts:
Main concepts which will allow to build object oriented model of brain:


Signals are different data objects which passed on input, emits on middle layers and gets as result.


Signal processor is some function which process the one type( or subtypes) of signals and emit signals to axon.  
Note: it has access to neuron and axon. 


Neuron is abstraction which store signals processor/processors for signal type/different signal types,  
 oder of signal type processing and axon. Note: neuron can be stateful or stateless.
 
 
Axon store connection to other neurons with weights for each type of signal.


Layer store the list of neurons which situated and input.


Result layer stores the final layer of neurons with associated result signals.


Studying algorithm has access  to all input/middle input/result and store studying logic.

Note: Result signal from one/many neuron nets can be used as input to other. Such approach will allow to "debug" AI and combine neuron nets.   

#Phases:
1. Make core. It will implement just core concepts without distributed mode and neuron nets synchronization.
2. Add simple java distributed part with neuron nets synchronization.
3. Add containers(docker/kubernetes) and infrastructure scripts python/shell.
4. Add neuron net graphic designer which will collect data about implemented classes and will pass to graphic plugin for eclipse and/or idea.
5. Add cuda distributed mode.
6. Add aws lambda distributed mode.
