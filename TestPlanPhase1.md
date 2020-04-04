Note: You can find example in sample package.
#Test steps:
1. Implement file IFileSystem and IFileSystemItem
Fill followed properties in config.properties
filesystem class name  
configuration.filesystem.class=  
contructor objects for file system constructor  
configuration.filesystem.constructor.args=  
class types for file system  constructor  
configuration.filesystem.constructor.args.types=
2.  Implement ISignal, ISignalProcessor,INConnection and IWeight. 
3. Implement IStudyingAlgorithm  and  IStudyingRequest.   
Serialize Studying Algorithm  to json and save to file.
4. Put the result from step 5 to config.properties   
configuration.studyingalgo=
5. Add input for first layer and create input folders for other layers
it should looks like $inputFolder/$layerId/file
file format {"inputs":[{"neuronId":"$longID","signal":{"className":"$signalClass",...}},...]}  
6. Put path to input dir to the  config.properties  
path to input folder  
configuration.input.path=  
7. create layers configuration in such format   
{"layerID":"$layerID", "layerSize":"$size","neurons":"$serializedToBytesArrayOfNeurons"}  
note: You must start layer from 0 and increment by 1 i.e input layer is 0
8. Place layers configuration in such way $layersPath/$fileNameAsLayerID
9. Specify path to layer configuration in config.properties  
path to neuron net configuration  
configuration.input.layermeta=


Fill free to contact me You will find errors/bugs or will need assist.
