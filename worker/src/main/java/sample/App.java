package sample;

import application.IRunner;

public class App {
    public static void main(String[] args) {
        IRunner runner = new SimpleRunner();
        runner.runNet("local");
       /* System.out.println("\""+NeuronLayerCreator.getFirstLayer()+"\"");
        System.out.println("\""+NeuronLayerCreator.getSecondLayerLayer()+"\"");
        System.out.println("\""+NeuronLayerCreator.getResultLayerLayer()+"\"");*/
        /*System.out.println("\""+NeuronLayerCreator.createInput()+"\"");
        System.out.println("\""+NeuronLayerCreator.getDesiredResult()+"\"");*/
    }
}
