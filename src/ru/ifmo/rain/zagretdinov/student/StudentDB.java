package ru.ifmo.rain.zagretdinov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {
    private static final Comparator<Student> STUDENT_BY_ID_COMPARATOR = Comparator
            .comparing(Student::getId, Integer::compareTo);
    private static final Comparator<Student> STUDENT_BY_NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparingInt(Student::getId);

    private String fullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private <T, C extends Collection<T>> C mapToCollection(Collection<Student> students, Function<Student, T> map,
                                                           Supplier<C> collector) {
        return students.stream().map(map).collect(Collectors.toCollection(collector));
    }

    private <T> List<T> mapToList(Collection<Student> students, Function<Student, T> map) {
        return mapToCollection(students, map, ArrayList::new);
    }

    private List<Student> filterAndSortByName(Collection<Student> students, Predicate<Student> condition) {
        return students.stream().filter(condition).sorted(STUDENT_BY_NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    private List<Student> sortToListBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    private List<Group> getGroups(Collection<Student> students, Comparator<Student> studentByNameComparator) {
        return streamSortedToGroup(students.stream(), studentByNameComparator).map
                (entry -> new Group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Group::getName, String::compareTo))
                .collect(Collectors.toList());
    }

    private <V> String maxWithComp(Stream<Map.Entry<String, V>> stream, Comparator<? super Map.Entry<String, V>> comp) {
        return stream.max(comp).map(Map.Entry::getKey).orElse("");
    }

    private String getLargestGroupBy(Stream<Map.Entry<String, List<Student>>> groups,
                                     Comparator<List<Student>> comparator) {
        return maxWithComp(groups, Map.Entry.<String, List<Student>>comparingByValue(comparator)
                .thenComparing(Map.Entry.comparingByKey(Collections.reverseOrder(String::compareTo))));
    }

    private Stream<Map.Entry<String, List<Student>>> streamToGroup(Stream<Student> students) {
        return students
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream();
    }

    private Stream<Map.Entry<String, List<Student>>> streamSortedToGroup(Stream<Student> students,
                                                                         Comparator<Student> comparator) {
        return streamToGroup(students.sorted(comparator));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroups(students, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroups(students, STUDENT_BY_ID_COMPARATOR);
    }


    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(streamToGroup(students.stream()), Comparator.comparingInt(List::size));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return maxWithComp(collectionToEntrySetStream(students, Student::getGroup,
                Collectors.mapping(Student::getFirstName, Collectors.collectingAndThen(Collectors.toSet(), Set::size))),
                Map.Entry.<String, Integer>comparingByValue().
                        thenComparing(Map.Entry.<String, Integer>comparingByKey().reversed()));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToList(students, this::fullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(STUDENT_BY_ID_COMPARATOR).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortToListBy(students, STUDENT_BY_ID_COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortToListBy(students, STUDENT_BY_NAME_COMPARATOR);
    }

    private <T, V> Predicate<T> resultEqualsTo(Function<T, V> function, V value) {
        return student -> Objects.equals(function.apply(student), value);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterAndSortByName(students, resultEqualsTo(Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterAndSortByName(students, resultEqualsTo(Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterAndSortByName(students, resultEqualsTo(Student::getGroup, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream().filter(resultEqualsTo(Student::getGroup, group)).
                collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    private <K, V> Stream<Map.Entry<K, V>> collectionToEntrySetStream(
            Collection<Student> students, Function<Student, K> func, Collector<Student, ?, V> collector) {
        return students.stream().collect(Collectors.groupingBy(func, collector)).entrySet().stream();
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return maxWithComp(collectionToEntrySetStream(students,
                this::fullName,
                Collectors.mapping(Student::getGroup, Collectors.toSet())),
                Map.Entry.<String, Set<String>>comparingByValue(Comparator.comparingInt(Set::size))
                        .thenComparing(Map.Entry.comparingByKey(String::compareTo)));
    }

    private <T> List<T> indicesToList(List<T> names, int[] indices) {
        return Arrays.stream(indices).mapToObj(names::get).collect(Collectors.toList());
    }

    private List<String> getByIndices(Collection<Student> students, int[] indices, Function<Student, String> function) {
        return mapToList(indicesToList(List.copyOf(students), indices), function);
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, this::fullName);
    }
}
