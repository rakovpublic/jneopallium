/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class LayerMove {

    private HashMap<Long, HashMap<Integer, List<Long>>> movingMap;

    private Integer layerRemoved;

    public LayerMove(HashMap<Long, HashMap<Integer, List<Long>>> movingMap, Integer layerRemoved) {
        this.movingMap = movingMap;
        this.layerRemoved = layerRemoved;
    }

    public Integer getLayerRemoved() {
        return layerRemoved;
    }

    public void setLayerRemoved(Integer layerRemoved) {
        this.layerRemoved = layerRemoved;
    }

    public HashMap<Long, HashMap<Integer, List<Long>>> getMovingMap() {
        return movingMap;
    }

    public void setMovingMap(HashMap<Long, HashMap<Integer, List<Long>>> movingMap) {
        this.movingMap = movingMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerMove layerMove = (LayerMove) o;
        return Objects.equals(movingMap, layerMove.movingMap) && Objects.equals(layerRemoved, layerMove.layerRemoved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movingMap, layerRemoved);
    }
}
