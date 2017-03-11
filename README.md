# Design Pattern Practice

This is a private project for practicing different design patterns.

## Strategy Pattern

The strategy pattern defines a family of algorithms, encapsulates each one, and makes them interchangeable.

Strategy lets the algorithm vary independently from clients that use it.

### Principles

- Identify the aspects of your application that vary and separate them from
what stays the same.
- Program to an interface, not an implementation.
- Favor composition over inheritance.

## Observer Pattern

The observer pattern defines a one-to-many dependency between objects so that when one object changes state, all of its dependents are notified and updated automatically.

### Principles

- Strive for loosely coupled designs between objects that interact.

## Decorator Pattern

The decorator pattern attaches additional responsibilities to an object dynamically.

Decorators provide a flexible alternative to subclassing for extending functionality.

### Principles

- Classes should be open for extension but closed for modification.

## Factory Method Pattern

The factory method pattern defines an interface for creating an object, but lets subclasses decide which class to instantiate. Factory method lets a class defer instantiation to subclasses.

### Principles

- Depend upon abstractions. Do not depend upon concrete classes.

## Command Pattern

The command pattern encapsulates a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations.
