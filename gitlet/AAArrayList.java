package gitlet;

import java.util.ArrayList;

/** A wrapper class of ArrayList<String>to avoid warnings
 *  and unchecked assignment.
 *  @author Wenhao Pan
 */
public class AAArrayList extends ArrayList<String> {

    /** A wrapper that passed in the ArrayList ARRAYLIST that we want to use. */
    public AAArrayList(ArrayList<String> arrayList) {
        _arrayList = arrayList;
    }

    @Override
    public void clear() {
        _arrayList.clear();
    }

    @Override
    public boolean isEmpty() {
        return _arrayList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return _arrayList.contains(o);
    }

    @Override
    public boolean remove(Object o) {
        return _arrayList.remove(o);
    }

    @Override
    public boolean add(String s) {
        return _arrayList.add(s);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return _arrayList.toArray(a);
    }

    /** Return _arraylist. */
    public ArrayList<String> getArrayList() {
        return _arrayList;
    }

    /** The content of the wrapper. */
    private ArrayList<String> _arrayList;
}
