#Project aim:
Purpose of the project is to build a natural neuron net modeling tool that allows building a neuron net model based on receptor existence probability, distance deviation between neurons with the same receptors, and receptor functional role.

#Project outputs:
Neuron net model sample.
Neuron net modeling tool.
Documentation for neuron net modeling tool.
Documentation for modeling process. 

#Current progress:
Neuron net modeling tool has passed pre-alpha test.
Tool architecture is ready.
Tool implementation is ready on 95%.

#Project impact
The project could lead to new generation of artificial neuron nets algorithms, help to analyze drug impact on neuron nets, build autonomous AI systems for different purposes, build modular AI, etc.



#Modeling process

1. Gather information about all neuron classes. Use the possibility of neurons to persist different types of information to separate and define neuron classes.
2. Gather information about all neuromediators and signals, and define signal classes. 
3. Investigate signal impact on neurons (receptors), and define processor classes.
4. Define neuron appearance probability for each layer.
5. Define receptor appearance probability for each neuron class.
6. Add connection generator and constraint rules which checks that neuron can have such connections.
7. Gather information about signal wide-spreading speed, and add to configuration (uses relative frequencies).
8. Implement the result layer
9. Add discriminators neuron nets


#Main features

Neuron net can process different types of signals (which allows modeling neuromediators behavior).
Different signals can be processed with different frequencies. It has 2 main frequency loops with an adjustable ratio like 1:n(which means that signals designed to process slowly will be processed once in n runs of fast processing signals). 
Signals can be continuous and be processed n runs with value change or without.
More over each signal type can be processed once in m runs of their loop.(it allows to model difference in signal widespreading)
Also, the framework supports a modular approach  to neuron net modeling where the output of neuron net can be the input of one or few neuron nets. Framework allows sending signals from the top-level neuron layer to input neuron nets. 
 

#Intro

I want to present to your attention my framework for neuron net building. The name of the framework is jneopallium (GitHub - rakovpublic/jneopallium: Tool for neuron net building.).
I have chosen this name because it's designed to allow the processing of the output of neuron nets as input for other neuron nets (I think it can be useful for debugging AI and a modular approach for AI building). Furthermore, if the input source is a neuron net, it is possible to send learning signals (signals that change the weights of dendrites, axons, delete, create, or update neurons).
The purpose of this framework is to give developers the ability to build object models of neuron structures.
In order to achieve this goal, I have developed a specific approach to defining the neuron, layer and input.




#Neuron.

In my framework, neurons have:
1. dendrites—an object that encapsulates input addresses, input signal types (class <? extends Signal interface>), and weight (an object that transforms signals and is used for learning ).After input signals have been processed through the weights by the signal processors.
2. signal processors are a specific class that has a method for processing specific input signals. processor has access to a neuron and can change it (for example, dendrites, axon or signal processor map). The output of signal processing is defined by the signal processing chain. The results of the signal processor are passed to the axon.
3. An axon is an object that encapsulates the addresses of consumer neurons and weights for signal transformation.
   Note: A signal can exist in more than one iteration of neuron net processing.

#Layer

A layer is a set of neurons. The maximum number of layers is Integer.MAX-1. 
The client developer can delete, update, or add neurons to the layer.
Delete and add neurons are accomplished by sending special signals (CreateNeuronSignal.class and DeleteNeuronSignal.class) to neurons (LayerManipulatingNeuron.class) on each layer with the id Long.MIN.
The maximum number of neurons in the layer is Long.MAX-1.
The service layer has an ID of Integer.MIN and is used to store information for cycling processing.
Cycling processing means that a neuron net will have a specified number of runs before getting the next input signals (used to wait for studying signal processing).

#Input

Input is compound and can have different input sources. Each source has a value that shows how often input from an input source should be populated into the neuron net. It also has a callback in case the input source is another neuron net and it can get a study signal from upstream.
The input strategy class defines how the input should be populated with neurons.
   Input is compound and can have different input sources. Each source has a value that shows how often input from an input source should be populated into the neuron net. It also has a callback in case the input source is another neuron net and it can get a study signal from upstream.
   The input strategy class defines how the input should be populated with neurons.


#Phases:
1. Make core. It will implement just core concepts without distributed mode and neuron nets synchronization.
2. Add simple java http distributed part with neuron nets synchronization.
3. Add grpc implementation for cluster mode
4. add Kafka input source implementation
5. Generate maven artifacts, host javadocs
6. Add containers(docker/kubernetes) and infrastructure scripts python/shell.
7. Add neuron net graphic designer which will collect data about implemented classes and will pass to graphic plugin for eclipse and (optional)idea.
8. Add redis as meta storage(optional).
9. Add aws lambda distributed mode(optional).
10. Design and implement amazon cluster integration(optional).

p.s. Fill free to contatct me. I am looking for contributors for this project.
p.p.s. Great thanks to kafedra of Informatics in Kharkiv National University of Radio and Electronics, Eugen Putiatin, Helen Matat, Tatiana Sinelnikova.
