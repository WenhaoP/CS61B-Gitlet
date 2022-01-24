package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

/** A wrapper class of HashMap<String, File> to avoid warnings
 *  and unchecked assignment.
 *  @author Wenhao Pan
 */
public class RemoteMap extends HashMap<String, File> {

    /** The real content inside the wrapper. */
    private HashMap<String, File> _map;

    /** A wrapper that passed in the Hashmap MAP that we want to use. */
    public RemoteMap(HashMap<String, File> map) {
        _map = map;
    }

    @Override
    public File get(Object key) {
        return _map.get(key);
    }

    @Override
    public File replace(String key, File value) {
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
    public File remove(Object key) {
        return _map.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return _map.remove(key, value);
    }

    @Override
    public File put(String key, File value) {
        return _map.put(key, value);
    }

    /** Return _map. */
    public HashMap<String, File> getMap() {
        return _map;
    }

}
