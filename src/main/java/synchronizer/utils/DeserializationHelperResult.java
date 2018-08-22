package synchronizer.utils;

public class DeserializationHelperResult {
    private String object;
    private int index;
    private String className;

    public DeserializationHelperResult(String object, int index, String className) {
        this.object = object;
        this.index = index;
        this.className = className;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
