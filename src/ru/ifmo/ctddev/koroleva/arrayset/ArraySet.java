package ru.ifmo.ctddev.koroleva.arrayset;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by Яна on 19.02.2015.
 */
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private T[] data;
    private int size;
    private int left;
    private int right;
    private boolean flagNaturalOrder = false;
    private Comparator<? super T> comparator;

    private ArraySet(T[] array, int left, int right, Comparator<? super T> comparator) {
        super();
        right = right < left ? left : right;

        this.data = array;
        this.left = left;
        this.right = right;
        this.size = right - left;
        this.comparator = comparator;
    }

    public ArraySet(T[] array) {
        this(array, null);
    }

    @SuppressWarnings("unchecked")
    public ArraySet(T[] array, Comparator<? super T> comparator) {
        super();
        setComparator(comparator);
        fromArrayToData(array);
    }

    @SuppressWarnings("unchecked")
    public ArraySet() {
        this.data = (T[]) new Object[0];
        this.left = this.right = this.size = 0;
    }


    public ArraySet(Collection<T> collection) {
        this(collection, null);
    }

    @SuppressWarnings("unchecked")
    public ArraySet(Collection<T> collection, Comparator<? super T> comparator) {
        super();
        setComparator(comparator);
        if (collection == null) {
            throw new NullPointerException();
        }
        T[] array = (T[])collection.toArray();
        fromArrayToData(array);
    }

    private void setComparator(Comparator<? super T> comparator) {
        if (comparator == null) {
            flagNaturalOrder = true;
            this.comparator = new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return ((Comparable<T>) o1).compareTo(o2);
                }
            };
        } else {
            this.comparator = comparator;
        }
    }

    @SuppressWarnings("unchecked")
    private void fromArrayToData(T[] array) {
        if (array == null) {
            throw new NullPointerException();
        }

        Arrays.sort(array, this.comparator);

        int current = 0;
        for (int i = 1; i < array.length; i++) {
            if (this.comparator.compare(array[i], array[current]) != 0) {
                array[++current] = array[i];
            }
        }
        if (array.length != 0)
            current++;

        this.data = (T[])Array.newInstance(array.getClass().getComponentType(), current);
        System.arraycopy(array, 0, this.data, 0, current);
        this.size = this.right = this.data.length;
        this.left = 0;
    }

    @Override
    public T lower(T t) {
        if (isEmpty())
            return null;

        int position = truePosition(t);
        return (position == left) ? null : data[position - 1];
    }

    @Override
    public T floor(T t) {
        if (isEmpty())
            return null;

        int position = truePosition(t);
        if (position != right && comparator.compare(data[position], t) == 0)
            return data[position];
        return (position == left) ? null : data[position - 1];
    }

    @Override
    public T ceiling(T t) {
        int position = truePosition(t);
        if(position == right)
            return null;
        return data[position];
    }

    @Override
    public T higher(T t) {
        int position = truePosition(t);
        if (position == right) {
            return null;
        }
        if (comparator.compare(data[position], t) == 0)
            return (position + 1 >= right) ? null : data[position + 1];
        return data[position];
    }

    private int truePosition(T t) {
        int position = Arrays.binarySearch(data, left, right, t, comparator);
        if (position < 0) {
            position = - position - 1;
        }
        return position;
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        int position = truePosition((T) o);
        return position != right && comparator.compare(data[position], (T) o) == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int position = left;
            @Override
            public boolean hasNext() {
                return position < right;
            }

            @Override
            public T next() {
                if (hasNext())
                    return data[position++];
                throw new NoSuchElementException();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public NavigableSet<T> descendingSet() {
        T[] reverseData = (T[]) new Object[size];
        System.arraycopy(data, left, reverseData, 0, size);
        NavigableSet<T> result = new ArraySet<T>(reverseData, Collections.reverseOrder(comparator));
        return result;
    }

    @Override
    public Iterator<T> descendingIterator() {
        return new Iterator<T>() {
            int position = right - 1;
            @Override
            public boolean hasNext() {
                return position >= left;
            }

            @Override
            public T next() {
                if (hasNext())
                    return data[position--];
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int left = truePosition(fromElement);
        if (left == this.right)
            return new ArraySet<T>(this.data, left, left, this.comparator);
        if (comparator.compare(data[left], fromElement) == 0 && !fromInclusive) {
            left++;
        }

        int right = truePosition(toElement);
        if (right < this.right && comparator.compare(data[right], toElement) == 0 && toInclusive) {
            right++;
        }

        return new ArraySet<T>(this.data, left, right, this.comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty())
            return this;
        return subSet(this.first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty())
            return this;
        return subSet(fromElement, inclusive, this.last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return flagNaturalOrder ? null : this.comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        if (isEmpty())
            return this;
        return subSet(this.first(), toElement);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        if (isEmpty())
            return this;
        return subSet(fromElement, true, this.last(), true);
    }

    @Override
    public T first() {
        if (size == 0)
            throw new NoSuchElementException();
        return data[left];
    }

    @Override
    public T last() {
        if (size == 0)
            throw new NoSuchElementException();
        return data[right - 1];
    }
}
