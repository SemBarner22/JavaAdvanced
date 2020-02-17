package ru.ifmo.rain.zagretdinov.walk;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private List<E> arrayList;
    private Comparator<? super E> comparator;

    public ArraySet() {
        arrayList = new ArrayList<>();
    }

    public ArraySet(Comparator<? super E> comparator) {
        arrayList = new ArrayList<>();
        this.comparator = comparator;
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.arrayList = list;
    }

    public ArraySet(Collection<E> c) {
        this.comparator = null;
        TreeSet<E> treeSet = new TreeSet<>();
        treeSet.addAll(c);
        arrayList = new ArrayList<>();
        arrayList.addAll(treeSet);
    }

    public ArraySet(Collection<E> c, Comparator<? super E> comparator) {
        this.comparator = comparator;
        TreeSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(c);
        arrayList = new ArrayList<>();
        arrayList.addAll(treeSet);
    }

    private int aroundIndex(E e, int shift) {
        int index = Collections.binarySearch(arrayList, e, comparator);
        if (index >= 0) {
            return index;
        } else {
            return (-index) - 1 - shift;
        }
    }

    private int aroundIndexStrict(E e, int shift, int bound) {
        int index = aroundIndex(e, shift);
        if (index != bound && comparator.compare(arrayList.get(index), e) == 0) {
            if (bound == -1) {
                --index;
            } else {
                ++index;
            }
        }
        return index;
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(arrayList, (E) Objects.requireNonNull(o), comparator) >= 0;
    }

    public E get(int i) {
        return arrayList.get(i);
    }

    private E getOrNull(int index) {
        if (index < 0 || index >= size()) {
            return null;
        } else {
            return arrayList.get(index);
        }
    }

    @Override
    public E lower(E e) {
        int index = aroundIndexStrict(e, 1, -1);
        return getOrNull(index);
    }

    @Override
    public E floor(E e) {
        int index = aroundIndex(e, 1);
        return getOrNull(index);
    }

    @Override
    public E ceiling(E e) {
        int index = aroundIndex(e, 0);
        return getOrNull(index);
    }

    @Override
    public E higher(E e) {
        int index = aroundIndexStrict(e, 0, size());
        return getOrNull(index);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        return arrayList.iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedArrayList<>(arrayList), comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedArrayList<>(arrayList).iterator();
    }

    @Override
    public NavigableSet<E> subSet(E e, boolean b, E e1, boolean b1) {
        int index, index1;
        if (b) {
            index = aroundIndex(e, 0);
        } else {
            index = aroundIndexStrict(e, 0, size());
        }
        if (b1) {
            index1 = aroundIndex(e1, 1);
        } else {
            index1 = aroundIndexStrict(e1, 1, -1);
        }
        if (index1 == -1 || index == size() || index > index1) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(arrayList.subList(index, index1 + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E e, boolean b) {
        if (size() == 0) {
            return new ArraySet<>(comparator);
        } else {
            return subSet(arrayList.get(0), true, e, b);
        }
    }

    @Override
    public NavigableSet<E> tailSet(E e, boolean b) {
        if (size() == 0) {
            return new ArraySet<>(comparator);
        } else {
            return subSet(e, b, arrayList.get(size() - 1), true);
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public SortedSet<E> subSet(E e, E e1) {
        return subSet(e, true, e1, false);
    }

    @Override
    public SortedSet<E> headSet(E e) {
        return headSet(e, false);
    }

    @Override
    public SortedSet<E> tailSet(E e) {
        return tailSet(e, true);
    }

    @Override
    public E first() {
        if (size() > 0) {
            return arrayList.get(0);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E last() {
        if (size() > 0) {
            return arrayList.get(size() - 1);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public int size() {
        return arrayList == null ? 0 : arrayList.size();
    }

    public class ReversedArrayList<T> extends AbstractList<T> {
        private List<T> arrayList;

        ReversedArrayList(List<T> arrayList) {
            this.arrayList = arrayList;
        }

        @Override
        public int size() {
            return arrayList.size();
        }

        @Override
        public List<T> subList(int i, int i1) {
            return arrayList.subList(i, i1);
        }

        @Override
        public T get(int i) {
            return arrayList.get(size() - i - 1);
        }
    }
}
