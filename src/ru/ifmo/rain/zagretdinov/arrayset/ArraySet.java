package ru.ifmo.rain.zagretdinov.arrayset;

import ru.ifmo.rain.zagretdinov.walk.WorkingCopy;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private List<E> list;
    private Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<E> c, Comparator<? super E> comparator) {
        this.comparator = comparator;
        NavigableSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(c);
        list = List.copyOf(treeSet);
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.list = list;
    }

    private void checkNull(E e) {
        Objects.requireNonNull(e);
    }

    private int getIndex(E e) {
        checkNull(e);
        return Collections.binarySearch(list, e, comparator);
    }

    private int index(E e, boolean inclusive, boolean lower) {
        int index = Collections.binarySearch(list, Objects.requireNonNull(e), comparator);
        if (index < 0) {
            return lower ? (-index - 1 - 1) : (-index - 1);
        } else {
            return inclusive ? index : (lower ? (index - 1) : (index + 1));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
//        try {
            return getIndex((E) o) >= 0;
//        } catch (ClassCastException | NullPointerException e) {
//            return false;
//        }
    }

    public E get(int i) {
        return list.get(i);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("ArraySet is unchangeable");
    }

    private E getOrNull(int index) {
        if (index < 0 || index >= size()) {
            return null;
        } else {
            return list.get(index);
        }
    }

    @Override
    public E lower(E e) {
        return getOrNull(index(e, false, true));
    }

    @Override
    public E floor(E e) {
        return getOrNull(index(e, true, true));
    }

    @Override
    public E ceiling(E e) {
        return getOrNull(index(e, true, false));
    }

    @Override
    public E higher(E e) {
        return getOrNull(index(e, false, false));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("ArraySet is unchangeable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("ArraySet is unchangeable");
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedArrayList<>(list), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E e, boolean b, E e1, boolean b1) {
        if ((comparator != null && comparator.compare(e, e1) > 0)  || comparator == null &&
                e instanceof Comparable && ((Comparable) e).compareTo(e1) > 0) {
            throw new IllegalArgumentException("Right bound should be greater than left");
        } else {
            return implSubSet(e, b, e1, b1);
        }
    }

    private NavigableSet<E> implSubSet(E e, boolean b, E e1, boolean b1) {
        if (e == null || e1 == null) {
            throw new IllegalArgumentException("Elements should not be null");
        }
        int index = index(e, b, false);
        int index1 = index(e1, b1, true);
        if (index > index1) {
            return new ArraySet<>(comparator);
        } else {
            return new ArraySet<>(list.subList(index, index1 + 1), comparator);
        }
    }


    @Override
    public NavigableSet<E> headSet(E e, boolean b) {
        return isEmpty() ? new ArraySet<>(comparator) : implSubSet(first(), true, e, b);
    }

    @Override
    public NavigableSet<E> tailSet(E e, boolean b) {
        return isEmpty() ? new ArraySet<>(comparator) : implSubSet(e, b, last(), true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator == Comparator.naturalOrder() ? null : comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
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


    private E firstLastImpl(int index) {
        if (!isEmpty()) {
            return list.get(index);
        } else {
            throw new NoSuchElementException("ArraySet is unchangeable");
        }
    }

    @Override
    public E first() {
        return firstLastImpl(0);
    }

    @Override
    public E last() {
        return firstLastImpl(size() - 1);
    }

    @Override
    public int size() {
        return list.size();
    }

    public class ReversedArrayList<T> extends AbstractList<T> implements RandomAccess {
        private List<T> arrayList;
        private boolean isReversed;

        ReversedArrayList(List<T> arrayList) {
            if (arrayList instanceof ReversedArrayList) {
                ReversedArrayList<T> list = (ReversedArrayList<T>) arrayList;
                this.arrayList = list.arrayList;
                this.isReversed = !list.isReversed;
            } else {
                this.arrayList = arrayList;
                this.isReversed = true;
            }
        }

        @Override
        public int size() {
            return arrayList.size();
        }

        @Override
        public T get(int i) {
            return arrayList.get(isReversed ? size() - i - 1 : i);
        }
    }
}
