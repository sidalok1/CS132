# Homework 2
## Filetree

    | Typecheck.java
    | TypeCheckVisitor.java
    \ utils
        | Edge.java
        | Graph.java
        | MethodType.java
        | TypeCheckFailed.java
        | TypeEnvironment.java
        | Types.java

Note that the java classes were written such that the JavaCC output is in the 
same directory, however the submission disallows this and required a different
format. The only changes between these files and the submissions are the import
statements.

## Typecheck.java
Contains the main function for the type checker
## TypeCheckVisitor.java
Contains the main visitor which recursively typechecks an abstract syntax tree.
This visitor class contain locally defined visitors and helper function, as well
as some globals.
## utils
### Edge.java
Used to implemented directed edges in a graph, and is intended to point from a
subclass to its superclass.
### Graph.java
The graph is used to implement an inheritance tree, making it convenient to find
the names of superclasses and check for cycles.
### MethodType.java
An implementation of a record which represents a function's type signature.
### TypeCheckFailed.java
Exception used for debugging convenience
### TypeEnvironment.java
A hashmap from variable identifiers to their types within the context.
### Types.java
A hybrid between a record class and enum, used to represent either one of the
MiniJava primitives, a defined class, or null.