package ru.ifmo.rain.zagretdinov.walk;

import java.util.*;

public class WorkingCopy {
    public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private List<E> arrayList;
        private Comparator<? super E> comparator;
        //private ReversedArrayList<E> reversedArrayList;

        public ArraySet() {
            arrayList = new ArrayList<>();
            //reversedArrayList = new ReversedArrayList<>(arrayList);
        }

        public ArraySet(Comparator<? super E> comparator) {
            arrayList = new ArrayList<>();
            this.comparator = comparator;
            //reversedArrayList = new ReversedArrayList<>(arrayList);
        }

        private ArraySet(List<E> list, Comparator<? super E> comparator) {
            this.comparator = comparator;
            this.arrayList = list;
            //reversedArrayList = new ReversedArrayList<>(arrayList);
        }

//    protected ArraySet(ReversedArrayList<E> reversedArrayList, Comparator<? super E> comparator) {
//        this.comparator = comparator;
//        this.arrayList = reversedArrayList;
//        reversedArrayList = new ReversedArrayList<>(arrayList);
//    }

        public ArraySet(Collection<E> c) {
            this.comparator = null;
            TreeSet<E> treeSet = new TreeSet<>();
            treeSet.addAll(c);
            arrayList = new ArrayList<>();
            arrayList.addAll(treeSet);
            //reversedArrayList = new ReversedArrayList<>(arrayList);
        }

        public ArraySet(Collection<E> c, Comparator<? super E> comparator) {
            this.comparator = comparator;
            TreeSet<E> treeSet = new TreeSet<>(comparator);
            treeSet.addAll(c);
            arrayList = new ArrayList<>();
            arrayList.addAll(treeSet);
            //reversedArrayList = new ReversedArrayList<>(arrayList);
        }


        private int aroundIndex(E e, int shift) {
            int index = Collections.binarySearch(arrayList, e, comparator);
            if (index >= 0) {
                return index;
            } else {
                return (-index) - 1 - shift;
            }
        }

        private int floorIndex(E e) {
            int index = Collections.binarySearch(arrayList, e, comparator);
            if (index >= 0) {
                return index;
            } else {
                return (-index) - 2;
            }
        }

        private int lowerIndex(E e) {
            int index = floorIndex(e);
            if (index != -1 && comparator.compare(arrayList.get(index), e) == 0) {
                --index;
            }
            return index;
        }

        @Override
        public boolean contains(Object o) {
            return Collections.binarySearch(arrayList, (E) o, comparator) >= 0;
        }

        private int ceilingIndex(E e) {
            int index = Collections.binarySearch(arrayList, e, comparator);
            if (index >= 0) {
                return index;
            } else {
                return (-(index) - 1);
            }
        }

        private int higherIndex(E e) {
            int index = ceilingIndex(e);
            if (index != size() && comparator.compare(arrayList.get(index), e) == 0) {
                ++index;
            }
            return index;
        }

        public E get(int i) {
            return arrayList.get(i);
        }

        @Override
        public E lower(E e) {
            int index = lowerIndex(e);
            if (index < 0) {
                return null;
            } else {
                return arrayList.get(index);
            }
        }

        @Override
        public E floor(E e) {
            int index = floorIndex(e);
            if (index < 0) {
                return null;
            } else {
                return arrayList.get(index);
            }
        }

        @Override
        public E ceiling(E e) {
            int index = ceilingIndex(e);
            if (index >= size()) {
                return null;
            } else {
                return arrayList.get(index);
            }
        }

        @Override
        public E higher(E e) {
            int index = higherIndex(e);
            if (index >= size()) {
                return null;
            } else {
                return arrayList.get(index);
            }
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
                index = ceilingIndex(e);
            } else {
                index = higherIndex(e);
            }
            if (b1) {
                index1 = floorIndex(e1);
            } else {
                index1 = lowerIndex(e1);
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

        public class ReversedArrayList<E> extends AbstractList<E> {
            private List<E> arrayList;

            ReversedArrayList(List<E> arrayList) {
                this.arrayList = arrayList;
            }

            @Override
            public int size() {
                return arrayList.size();
            }

            @Override
            public List<E> subList(int i, int i1) {
                return arrayList.subList(i, i1);
            }

            @Override
            public E get(int i) {
                return arrayList.get(size() - i - 1);
            }
        }
    }
}