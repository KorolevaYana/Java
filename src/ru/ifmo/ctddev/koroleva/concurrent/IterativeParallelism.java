package ru.ifmo.ctddev.koroleva.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import ru.ifmo.ctddev.koroleva.mapper.ParallelMapperImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Class implements methods of interface {@link info.kgeorgiy.java.advanced.concurrent.ListIP}, which
 * do some operations with list in parallel.
 */

public class IterativeParallelism implements ListIP {

    /**
     * Just empty constructor. Does nothing.
     */
    public IterativeParallelism() {}

    private ParallelMapper mapper = null;

    /**
     * Constructor from ParallelMapper.
     *
     * @param mapper mapper to set.
     *
     * @see ru.ifmo.ctddev.koroleva.mapper.ParallelMapperImpl
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @SuppressWarnings("unchecked")
    private <R, T, E> E splitToThreads(int i, List<? extends R> list,
                                       Function<List<? extends R>, T> functionOnThread, Function<List<T>, E> functionToMerge)
            throws InterruptedException {

        int cnt = Math.min(i, list.size());
        List<List<? extends R>> sublists = new ArrayList<>();
        List<T> results;

        for (int j = 0; j < cnt - 1; j++) {
            sublists.add(list.subList(j * (list.size() / cnt), (j + 1) * (list.size() / cnt)));
        }
        sublists.add(list.subList((list.size() / cnt) * (cnt - 1), list.size()));

        if (mapper == null) {
            ParallelMapperImpl localMapper = new ParallelMapperImpl(i);
            results = localMapper.map(functionOnThread, sublists);
        } else {
            results = mapper.map(functionOnThread, sublists);
        }
        return functionToMerge.apply(results);
    }

    /**
     * Concatenate string values of <tt>list</tt> elements.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list, which values should be concatenated.
     * @return concatenations of list elements.
     * @throws InterruptedException if something is wrong with threads.
     */
    @Override
    public String concat(int i, List<?> list)
            throws InterruptedException {
        return splitToThreads(i, list,
                (laterList) -> {
                    StringBuilder builder = new StringBuilder();
                    for (Object aLaterList : laterList) {
                        builder.append(aLaterList.toString());
                    }
                    return builder;
                },
                (results) -> {
                    StringBuilder result = new StringBuilder();
                    results.forEach(result::append);
                    return result.toString();
                });
    }

    /**
     * Returns elements of <tt>list</tt>, on which <tt>predicate</tt> returns <tt>true</tt>.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list of elements to apply predicate.
     * @param predicate predicate for analysing elements.
     * @param <T> type of elements of list.
     * @return list of elements, on which predicate returns true.
     * @throws InterruptedException if something is wrong with threads.
     */
    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return splitToThreads(i, list,
                (laterList) -> {
                    List<T> result = new ArrayList<>();
                    for (T element : laterList) {
                        if (predicate.test(element)) {
                            result.add(element);
                        }
                    }
                    return result;
                },
                (results) -> {
                    List<T> result = new ArrayList<>();
                    for (List<T> element : results) {
                        result.addAll(element);
                    }
                    return result;
                });
    }

    /**
     * Returns results of applying <tt>function</tt> on each element of <tt>list</tt>.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list, on which elements function should be applied.
     * @param function function to apply.
     * @param <T> type of elements of given list.
     * @param <U> type of returning value of function.
     * @return list of results of applying function on elements of list.
     * @throws InterruptedException if something is wrong with threads.
     */
    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function)
            throws InterruptedException {
        return splitToThreads(i, list,
                (laterList) -> {
                    ArrayList<U> result = new ArrayList<>();
                    for (T element : laterList) {
                        result.add(function.apply(element));
                    }
                    return result;
                },
                (results) -> {
                    ArrayList<U> result = new ArrayList<>();
                    for (List<U> element : results) {
                        result.addAll(element);
                    }
                    return result;
                }
            );
    }

    /**
     * Returns the maximum element of <tt>list</tt>, using <tt>comparator</tt>.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list, where we should find maximum.
     * @param comparator comparator for comparing elements of list.
     * @param <T> type of elements of list.
     * @return maximum of elements of list.
     * @throws InterruptedException if something is wrong with threads.
     *
     * @see #minimum minimum
     */
    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator)
            throws InterruptedException {
        return splitToThreads(i, list,
                (laterList) -> {
                    T max = laterList.get(0);
                    for (int j = 1; j < laterList.size(); j++) {
                        if (comparator.compare(max, laterList.get(j)) < 0) {
                            max = laterList.get(j);
                        }
                    }
                    return max;
                },
                (results) -> {
                    if (results != null) {
                        T max = results.get(0);
                        for (T element : results) {
                            if (element != null) {
                                if (comparator.compare(max, element) < 0) {
                                    max = element;
                                }
                            }
                        }
                        return max;
                    }
                    return null;
                });
    }

    /**
     * Returns the minimum element of <tt>list</tt>, using <tt>comparator</tt>.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list, where we should find minimum.
     * @param comparator comparator for comparing elements of list.
     * @param <T> type of elements of list.
     * @return minimum of elements of list.
     * @throws InterruptedException if something is wrong with threads.
     *
     * @see #maximum maximum
     */
    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(i, list, comparator.reversed());
    }

    /**
     * Return <tt>true</tt>, if all elements returns <tt>true</tt> on <tt>predicate</tt>.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list, on which element predicate should be applied.
     * @param predicate predicate to apply.
     * @param <T> type of list elements.
     * @return returns true, if all elements returns true on predicate, and false otherwise.
     * @throws InterruptedException if something is wrong with threads.
     *
     * @see #any any
     */
    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return splitToThreads(i, list,
                (laterList) -> {
                    boolean result = true;
                    for (T element : laterList) {
                        result &= predicate.test(element);
                    }
                    return result;
                },
                (results) -> {
                    boolean result = true;
                    for (boolean element : results) {
                        result &= element;
                    }
                    return result;
                });
    }

    /**
     * Return <tt>true</tt>, if one or more elements returns <tt>true</tt> on <tt>predicate</tt>.
     *
     * @param i maximum number of threads, which we have to use.
     * @param list list, on which element predicate should be applied.
     * @param predicate predicate to apply.
     * @param <T> type of list elements.
     * @return returns true, if one or more elements returns true on predicate, and false otherwise.
     * @throws InterruptedException if something is wrong with threads.
     *
     * @see #all all
     */
    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(i, list, predicate.negate());
    }
}



