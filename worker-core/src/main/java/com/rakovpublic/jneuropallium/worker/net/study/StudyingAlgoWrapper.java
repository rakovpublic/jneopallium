package com.rakovpublic.jneuropallium.worker.net.study;

public class StudyingAlgoWrapper {
    private String className;
    private ILearningAlgo studyingAlgo;

    public StudyingAlgoWrapper(String className, ILearningAlgo studyingAlgo) {
        this.className = className;
        this.studyingAlgo = studyingAlgo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ILearningAlgo getStudyingAlgo() {
        return studyingAlgo;
    }

    public void setStudyingAlgo(ILearningAlgo studyingAlgo) {
        this.studyingAlgo = studyingAlgo;
    }
}
