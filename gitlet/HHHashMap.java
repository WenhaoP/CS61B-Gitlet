package gitlet;

import java.util.HashMap;
import java.util.Set;

/** A wrapper class of HashMap<String, String> to avoid warnings
 *  and unchecked assignment.
 *  @author Wenhao Pan
 */
public class HHHashMap extends HashMap<String, String> {

    /** The real content inside the wrapper. */
    private HashMap<String, String> _map;

    /** A wrapper that passed in the Hashmap MAP that we want to use. */
    public HHHashMap(HashMap<String, String> map) {
        _map = map;
    }

    @Override
    public String get(Object key) {
        return _map.get(key);
    }

    @Override
    public String replace(String key, String value) {
        return _map.replace(key, value);
    }

    @Override
    public void clear() {
        _map.clear();
    }

    @Override
    public boolean isEmpty() {
        return _map.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return _map.keySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return _map.containsKey(key);
    }

    @Override
    public String remove(Object key) {
        return _map.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return _map.remove(key, value);
    }

    @Override
    public String put(String key, String value) {
        return _map.put(key, value);
    }

    /** Return _map. */
    public HashMap<String, String> getMap() {
        return _map;
    }

}
