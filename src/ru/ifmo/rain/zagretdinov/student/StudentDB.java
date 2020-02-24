package ru.ifmo.rain.zagretdinov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {
    private static final Comparator<Student> STUDENT_BY_ID_COMPARATOR = Comparator
            .comparing(Student::getId, Integer::compareTo);
    private static final Comparator<Student> STUDENT_BY_NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparingInt(Student::getId);  


    private <T> List<T> mapToList(Collection<Student> students, Function<Student, T> map) {  
        return students.stream().map(map).collect(Collectors.toList());
    }

    private Stream<Student> filterToStream(Stream<Student> students, Predicate<Student> condition) {  
        return students.filter(condition);
    }

    private List<Student> filterToList(Collection<Student> students, Predicate<Student> condition) {  
        return students.stream().filter(condition).collect(Collectors.toList());
    }

    private List<Student> filterAndSort(Collection<Student> students, Predicate<Student> condition) {  
        return filterToStream(students.stream(), condition).sorted(STUDENT_BY_NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    private List<Student> sortToListBy(Collection<Student> students, Comparator<Student> comparator) {  
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    private <T, C extends Collection<T>> C mapToCollection(Collection<Student> students, Function<Student, T> map,
                                                           Supplier<C> collector) {  
        return students.stream().map(map).collect(Collectors.toCollection(collector));
    }

    private List<Group> getGroups(Collection<Student> students, Comparator<Student> studentByNameComparator) {  
        return streamSortedToGroup(students.stream(), studentByNameComparator).map
                (entry -> new Group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Group::getName, String::compareTo))
                .collect(Collectors.toList());
    }

    private String getLargestGroupBy(Stream<Map.Entry<String, List<Student>>> groups,
                                     Comparator<List<Student>> comparator) {
        return groups
                .max(Map.Entry.<String, List<Student>>comparingByValue(comparator)
                        .thenComparing(Map.Entry.comparingByKey(Collections.reverseOrder(String::compareTo))))
                .map(Map.Entry::getKey)
                .orElse("");
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
        return getLargestGroupBy(streamToGroup(students.stream()),
                Comparator.comparingInt(list -> getDistinctFirstNames(list).size()));
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
        return mapToList(students, s -> s.getFirstName() + " " + s.getLastName());
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
        return sortToListBy(students, Comparator.comparing(Student::getLastName).
                thenComparing(Student::getFirstName).thenComparing(Student::getId));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {  
        return filterToList(students, s -> s.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {  
        return filterToList(students, s -> s.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {  
        return filterAndSort(students, (s -> s.getGroup().equals(group)));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {  
        return filterToStream(students.stream(), student -> student.getGroup().equals(group)).
                collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }
}
