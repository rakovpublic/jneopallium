package synchronizer.utils;

public class JSONHelper {
    public static JSONHelperResult getNextJSONObject(String json, Integer index) {

        JSONHelperResult res = null;
        String result = null;
        index = json.indexOf(index, '{');
        if (index == -1 || index >= json.length()) {
            return null;
        }
        int open = 0;
        int startIndex = index;
        for (int i = index; i < json.length(); i++) {
            if (json.charAt(i) == '{') {
                open += 1;
            } else if (json.charAt(i) == '}') {
                open -= 1;
            }
            if (open == 0) {

                result = json.substring(startIndex, i) + "}";
                res = new JSONHelperResult(result, i, extractJsonField(result, "className"));
            }
        }
        return res;
    }
    public static JSONObjectMeta getJSONObjectByFieldValue(String json, String field, String value){
        return null;
    }

    public static String extractJsonField(String json, String fieldName) {

        int index = json.indexOf(fieldName);
        index = json.indexOf(':', index);
        int endIndex = json.indexOf('"', json.indexOf('"', index) + 1);
        if (index + 2 < endIndex - 1) {
            return json.substring(index + 2, endIndex);
        }
        return "";

    }
}
