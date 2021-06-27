package com.rakovpublic.jneuropallium.worker.net.study;

public class StudyingAlgoWrapper {
    private String className;
    private IStudyingAlgo studyingAlgo;

    public StudyingAlgoWrapper(String className, IStudyingAlgo studyingAlgo) {
        this.className = className;
        this.studyingAlgo = studyingAlgo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public IStudyingAlgo getStudyingAlgo() {
        return studyingAlgo;
    }

    public void setStudyingAlgo(IStudyingAlgo studyingAlgo) {
        this.studyingAlgo = studyingAlgo;
    }
}
