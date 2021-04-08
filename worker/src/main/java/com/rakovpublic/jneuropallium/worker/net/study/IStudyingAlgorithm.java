package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.net.storages.IStructMeta;

import java.io.Serializable;
import java.util.List;

/**
 * Studying algorithm for classical approaches note: signal based studying can be implemented without studying algorithm
 */
public interface IStudyingAlgorithm extends Serializable {
    List<IStudyingRequest> study(IStructMeta structMeta,Long neuronId);
}
