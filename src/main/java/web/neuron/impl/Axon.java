package web.neuron.impl;

import web.neuron.IAxon;
import web.neuron.INConnection;
import web.neuron.IWeight;
import web.signals.ISignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Axon implements IAxon {
    private HashMap<Class<? extends ISignal>, List<INConnection>> connectionMap;

    public Axon() {
        this.connectionMap = new HashMap<>();
    }

    @Override
    public <S extends ISignal> void putConnection(Class<S> cl, INConnection<S> connection) {
        if(connectionMap.containsKey(cl)){
            connectionMap.get(cl).add(connection);
        }else{
            List<INConnection> tlist= new ArrayList<>();
            tlist.add(connection);
            connectionMap.put(cl,tlist);
        }
    }



    @Override
    public void cleanConnections() {
        connectionMap.clear();


    }

    @Override
    public HashMap<ISignal, List<INConnection>> processSignal(List<ISignal> signal) {
        HashMap<ISignal,List<INConnection>> result=new HashMap<>();
        for(ISignal s:signal){
            Class<? extends ISignal>  cl= s.getCurrentClass();
            if(connectionMap.containsKey(cl)){
                for(INConnection con:connectionMap.get(cl)){
                    ISignal resSignal= con.getWeight().process(s);
                    if(result.containsKey(resSignal)){
                        result.get(resSignal).add(con);
                    }else{
                        List<INConnection> cons= new ArrayList<>();
                        cons.add(con);
                        result.put(resSignal,cons);
                    }
                }
            }else {

            }
        }
        return result;
    }

    @Override
    public String toJSON() {
        return null;
    }
}
