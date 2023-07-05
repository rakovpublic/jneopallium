Project aim:
Purpose of the project is build neuron net model based on receptor existence probability, distance deviation between neuron with same receptors and receptor functional role.

Project outputs:
Neuron net model.
Neuron net modeling tool.
Documentation for neuron net modeling tool.
Documentation for modeling process. 

Current progress:
Neuron net modeling tool has passed pre-alpha test.
Tool architecture is ready on 95%.
Tool implementation is ready on 75%.

Project impact
Project could lead to new generation of artificial neuron nets algorithms, help to analyze drug impact on neuron nets, build autonomous ai systems for different purposes, build modular ai, etc.

#Main features

Neuron net can process different types of signals (which allows to model neuromediators behaviour ).
Different signals can be processed with different frequency. It has 2 main frequency loops with adjustable ratio like 1:n(which mean that signals designed to process slowly will be processed once in n runs of fast processing signals). 
Signals can be continuous and be processed n runs with value change or without.
More over each signal type can be processed once in m runs of their loop.(it allows to model difference in signal widespreading)
Also the framework supports modular approach  to neuron net modeling where output of neuron net can be input of one or few neuron nets. Framework allows to send signals from top level neuron net to input neuron nets. 
 

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

#Learning 

#Supervised learning
   In order to implement this type of learning client should define comparing strategy which will compare actual result with desired and return neuron ids to change and learning algorythm which will change the weight.
   Then add it to configuration.

#Unsupervised or reinforced learning
This  is the easiest part and does not required any additional code except signals and processors definition.

#Phases:
1. Make core. It will implement just core concepts without distributed mode and neuron nets synchronization.
2. Add simple java http distributed part with neuron nets synchronization.
3. Add grpc implementation for cluster mode
4. Generate maven artifacts, host javadocs
5. Add containers(docker/kubernetes) and infrastructure scripts python/shell.
6. Add queues distributed part (optional)
7. Design and implement open stack cluster integration(optional).
8. Add neuron net graphic designer which will collect data about implemented classes and will pass to graphic plugin for eclipse and (optional)idea.
9. Add cuda distributed mode(optional).
10. Add aws lambda distributed mode.
11. Design and implement amazon cluster integration(optional).

p.s. Fill free to contatct me. I am looking for contributors for this project.
p.p.s. Great thanks to kafedra of Informatics in Kharkiv National University of Radio and Electronics, Eugen Putiatin, Helen Matat, Tatiana Sinelnikova.
