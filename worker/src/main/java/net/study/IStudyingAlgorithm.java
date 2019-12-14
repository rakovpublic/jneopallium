package net.study;

import synchronizer.IContext;

import java.io.Serializable;
import java.util.List;

public interface IStudyingAlgorithm extends Serializable {
    List<IStudyingRequest> study(IContext context);
}
