package sample;

import application.IRunner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.neuron.INeuron;
import net.neuron.impl.Neuron;
import sample.SimpleRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

public class App {
    public static void main(String [] args){
        /*IRunner runner= new SimpleRunner();
        runner.runNet(args[0]);*/
        System.out.println("\""+NeuronLayerCreator.getFirstLayer()+"\"");
        System.out.println("\""+NeuronLayerCreator.getSecondLayerLayer()+"\"");
        System.out.println("\""+NeuronLayerCreator.getResultLayerLayer()+"\"");
    }
}
