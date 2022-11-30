package com.smfreports;

import java.util.*;

public class Finished implements List<Object> 
{
    private static final List<Object> emptyList = Collections.emptyList();

    @Override
    public int size() {
        return emptyList.size();
    }

    @Override
    public boolean isEmpty() {
        return emptyList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return emptyList.contains(o);
    }

    @Override
    public Iterator<Object> iterator() {
        return emptyList.iterator();
    }

    @Override
    public Object[] toArray() {
        return emptyList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return emptyList.toArray(a);
    }

    @Override
    public boolean add(Object e) {
        return emptyList.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return emptyList.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return emptyList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Object> c) {
        return emptyList.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Object> c) {
        return emptyList.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return emptyList.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return emptyList.retainAll(c);
    }

    @Override
    public void clear() {
        emptyList.clear();
    }

    @Override
    public Object get(int index) {
        return emptyList.get(index);
    }

    @Override
    public Object set(int index, Object element) {
        return emptyList.set(index, element);
    }

    @Override
    public void add(int index, Object element) {
        emptyList.add(index, element);
    }

    @Override
    public Object remove(int index) {
        return emptyList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return emptyList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return emptyList.lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return emptyList.listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return emptyList.listIterator(index);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return emptyList.subList(fromIndex, toIndex);
    }
}
