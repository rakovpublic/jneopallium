package sample;

import application.IRunner;
import sample.SimpleRunner;

public class App {
    public static void main(String [] args){
        IRunner runner= new SimpleRunner();
        runner.runNet(args[0]);
    }
}
