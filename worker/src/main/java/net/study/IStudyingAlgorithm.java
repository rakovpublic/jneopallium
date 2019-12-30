package net.study;

import net.storages.ILayersMeta;
import net.storages.IStructMeta;
import synchronizer.IContext;

import java.io.Serializable;
import java.util.List;

public interface IStudyingAlgorithm extends Serializable {
    List<IStudyingRequest> study(IStructMeta structMeta);
}
