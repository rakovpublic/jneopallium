#Test steps:
1. Implement file IFileSystem and IFileSystemItem
Fill followed properties in config.properties
filesystem class name  
configuration.filesystem.class=  
contructor objects for file system constructor  
configuration.filesystem.constructor.args=  
class types for file system  constructor  
configuration.filesystem.constructor.args.types=
2.  Implement ISignal, ISignalProcessor, ISignalSerializer,INConnection and IWeight. 
3. Create  HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>>    
serialize to bytes and save to file.
You can use for this purpose ByteSaverHelper.
4. Put the result from step 3 to config.properties  
signal serializers in byte view  
configuration.serializers=
5. Implement IStudyingAlgorithm  and  IStudyingRequest.   
Serialize Studying Algorithm  to bytes and save to file.
6. Put the result from step 5 to config.properties   
Studying algo in byte view  
configuration.studyingalgo=
7. Add input for first layer and create input folders for other layers
it should looks like $inputFolder/$layerId/file
file format {"inputs":[{"neuronId":"$longID","signal":{"className":"$signalClass",...}},...]}  
Note: ISignalSerializer which You have implemented in step 2 should serialize/deserialize ISignal with field className
Result object for studying should be stored her as bytes $inputFolder/result use ByteSaverHelper
8. Put path to input dir to the  config.properties  
path to input folder  
configuration.input.path=  
9. create layers configuration in such format   
{"layerID":"$layerID", "layerSize":"$size","neurons":"$serializedToBytesArrayOfNeurons"}  
note: You must start layer from 0 and increment by 1 i.e input layer is 0
10. Place layers configuration in such way $layersPath/$fileNameAsLayerID
11. Specify path to layer configuration in config.properties  
path to neuron net configuration  
configuration.input.layermeta=
