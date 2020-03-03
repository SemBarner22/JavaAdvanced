package ru.ifmo.rain.zagretdinov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final ReversedArrayList<E> array;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        NavigableSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        this.array = new ReversedArrayList<>(new ArrayList<>(treeSet));
        this.comparator = getValidComparator(comparator);
    }

    private ArraySet(List<E> array, Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.array = new ReversedArrayList<>(array);
    }

    private Comparator<? super E> getValidComparator(Comparator<? super E> cmp) {
        return Comparator.naturalOrder() == cmp ? null : cmp;
    }

    private E checkNull(E e) {
        return Objects.requireNonNull(e, "Expected not null element");
    }

    private int getIndex(E e) {
        return Collections.binarySearch(array, checkNull(e), comparator);
    }

    private int index(E e, boolean inclusive, boolean lower) {
        int index = getIndex(e);
        if (index < 0) {
            return lower ? (-index - 1 - 1) : (-index - 1);
        } else {
            return inclusive ? index : (lower ? (index - 1) : (index + 1));
        }
    }

    @Override
    public Iterator<E> iterator() {
        return array.iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return getIndex((E) o) >= 0;
    }

    public E get(int i) {
        return array.get(i);
    }

    private E getOrNull(int index) {
        return (0 <= index && index < size()) ? array.get(index) : null;
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
        throw new UnsupportedOperationException(
                "Poll First is not supported, ArraySet is unchangeable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException(
                "Poll Last is not supported, ArraySet is unchangeable"
        );
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedArrayList<E>(array),
                Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E from, boolean isFromIncluded, E to, boolean isToIncluded) {
        if (compare(from, to) > 0) {
            throw new IllegalArgumentException("Left element shouldn't be greater than right");
        }
        return implSubSet(from, isFromIncluded, to, isToIncluded);
    }

    @SuppressWarnings("unchecked")
    private int compare(E e1, E e2) {
        return (comparator == null) ? ((Comparable<E>) e1).compareTo(e2) : comparator.compare(e1, e2);
    }

    private NavigableSet<E> implSubSet(E from, boolean isFromIncluded, E to, boolean isToIncluded) {
        checkNull(from);
        checkNull(to);
        int indexFrom = index(from, isFromIncluded, false);
        int indexTo = index(to, isToIncluded, true);
        if (indexFrom > indexTo) {
            return new ArraySet<>(comparator);
        } else {
            return new ArraySet<>(array.subList(indexFrom, indexTo + 1), comparator);
        }
    }

    @Override
    public NavigableSet<E> headSet(E e, boolean b) {
        return isEmpty() ? this : implSubSet(first(), true, e, b);
    }

    @Override
    public NavigableSet<E> tailSet(E e, boolean b) {
        return isEmpty() ? this : implSubSet(e, b, last(), true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator == Comparator.naturalOrder() ? null : comparator;
    }

    @Override
    public SortedSet<E> subSet(E from, E to) {
        return subSet(from, true, to, false);
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
            return array.get(index);
        } else {
            throw new NoSuchElementException("ArraySet is empty");
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
        return array.size();
    }

    private class ReversedArrayList<T> extends AbstractList<T> implements RandomAccess {
        private final List<T> arrayList;
        private boolean isReversed;

        private ReversedArrayList(List<T> other, boolean isReversed) {
            arrayList = Collections.unmodifiableList(other);
            this.isReversed = isReversed;
        }

        private ReversedArrayList(List<T> other) {
            this(other, false);
        }

        private ReversedArrayList(ReversedArrayList<T> other) {
            this(other.arrayList, !other.isReversed);
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
