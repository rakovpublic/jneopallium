package net.study;

import synchronizer.IContext;

import java.util.List;

public interface IStudyingAlgorithm {
    List<IStudyingRequest> study(IContext context);
}
