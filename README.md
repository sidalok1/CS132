# CS132
## Homeworks/Projects for CS 132

CS 132, Compiler Construction, taught by professor Palsberg during Spring quarter 2025 at UCLA.

The ultimate goal for the project is to build a MiniJava to RISC-V compiler. Sparrow/Sparrow-V is used as and 
intermediate representation. All work is done in Java.
While still relevant, [Homework 1](#homework-1) does not directly contribute to the project, which is covered by homeworks 
[2](#homework-2), [3](), [4](), and [5]().

---

### Homework 1
#### Implement a Recursive Descent Parser

Using the principles taught on [LL(1) Academy](http://ll1academy.cs.ucla.edu/), a parser for a subset of MiniJava is 
designed. The parser can be called from the following command:

```shell
  java Parse < program
```

Specifically, the parser is for the following grammar:
```
S ::= { L } | System.out.println ( E ) ; | if ( E ) S else S | while ( E ) S
L ::= S L | ϵ
E ::= true | false | ! E
```
---
### Homework 2
#### Implement a MiniJava Type Checker

Using the following grammar in conjunction with the type rules given in 
*The MiniJava Type System* document, a type checker is implemented

<TABLE>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod1">Goal</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod2">MainClass</A> ( <A HREF="#prod3">TypeDeclaration</A> )* &lt;EOF&gt;</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod2">MainClass</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"class" <A HREF="#prod4">Identifier</A> "{" "public" "static" "void" "main" "(" "String" "[" "]" <A HREF="#prod4">Identifier</A> ")" "{" ( <A HREF="#prod5">VarDeclaration</A> )* ( <A HREF="#prod6">Statement</A> )* "}" "}"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod3">TypeDeclaration</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod7">ClassDeclaration</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod8">ClassExtendsDeclaration</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod7">ClassDeclaration</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"class" <A HREF="#prod4">Identifier</A> "{" ( <A HREF="#prod5">VarDeclaration</A> )* ( <A HREF="#prod9">MethodDeclaration</A> )* "}"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod8">ClassExtendsDeclaration</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"class" <A HREF="#prod4">Identifier</A> "extends" <A HREF="#prod4">Identifier</A> "{" ( <A HREF="#prod5">VarDeclaration</A> )* ( <A HREF="#prod9">MethodDeclaration</A> )* "}"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod5">VarDeclaration</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod10">Type</A> <A HREF="#prod4">Identifier</A> ";"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod9">MethodDeclaration</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"public" <A HREF="#prod10">Type</A> <A HREF="#prod4">Identifier</A> "(" ( <A HREF="#prod11">FormalParameterList</A> )? ")" "{" ( <A HREF="#prod5">VarDeclaration</A> )* ( <A HREF="#prod6">Statement</A> )* "return" <A HREF="#prod12">Expression</A> ";" "}"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod11">FormalParameterList</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod13">FormalParameter</A> ( <A HREF="#prod14">FormalParameterRest</A> )*</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod13">FormalParameter</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod10">Type</A> <A HREF="#prod4">Identifier</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod14">FormalParameterRest</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"," <A HREF="#prod13">FormalParameter</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod10">Type</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod15">ArrayType</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod16">BooleanType</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod17">IntegerType</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod4">Identifier</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod15">ArrayType</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"int" "[" "]"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod16">BooleanType</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"boolean"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod17">IntegerType</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"int"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod6">Statement</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod18">Block</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod19">AssignmentStatement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod20">ArrayAssignmentStatement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod21">IfStatement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod22">WhileStatement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod23">PrintStatement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod18">Block</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"{" ( <A HREF="#prod6">Statement</A> )* "}"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod19">AssignmentStatement</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod4">Identifier</A> "=" <A HREF="#prod12">Expression</A> ";"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod20">ArrayAssignmentStatement</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod4">Identifier</A> "[" <A HREF="#prod12">Expression</A> "]" "=" <A HREF="#prod12">Expression</A> ";"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod21">IfStatement</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"if" "(" <A HREF="#prod12">Expression</A> ")" <A HREF="#prod6">Statement</A> "else" <A HREF="#prod6">Statement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod22">WhileStatement</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"while" "(" <A HREF="#prod12">Expression</A> ")" <A HREF="#prod6">Statement</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod23">PrintStatement</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"System.out.println" "(" <A HREF="#prod12">Expression</A> ")" ";"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod12">Expression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod24">AndExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod25">CompareExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod26">PlusExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod27">MinusExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod28">TimesExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod29">ArrayLookup</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod30">ArrayLength</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod31">MessageSend</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod24">AndExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "&amp;&amp;" <A HREF="#prod32">PrimaryExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod25">CompareExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "&lt;" <A HREF="#prod32">PrimaryExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod26">PlusExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "+" <A HREF="#prod32">PrimaryExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod27">MinusExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "-" <A HREF="#prod32">PrimaryExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod28">TimesExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "*" <A HREF="#prod32">PrimaryExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod29">ArrayLookup</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "[" <A HREF="#prod32">PrimaryExpression</A> "]"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod30">ArrayLength</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "." "length"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod31">MessageSend</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod32">PrimaryExpression</A> "." <A HREF="#prod4">Identifier</A> "(" ( <A HREF="#prod33">ExpressionList</A> )? ")"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod33">ExpressionList</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod12">Expression</A> ( <A HREF="#prod34">ExpressionRest</A> )*</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod34">ExpressionRest</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"," <A HREF="#prod12">Expression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod32">PrimaryExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod35">IntegerLiteral</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod36">TrueLiteral</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod37">FalseLiteral</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod4">Identifier</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod38">ThisExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod39">ArrayAllocationExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod40">AllocationExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod41">NotExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>|</TD>
<TD ALIGN=LEFT VALIGN=BASELINE><A HREF="#prod42">BracketExpression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod35">IntegerLiteral</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;INTEGER_LITERAL&gt;</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod36">TrueLiteral</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"true"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod37">FalseLiteral</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"false"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod4">Identifier</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>&lt;IDENTIFIER&gt;</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod38">ThisExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"this"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod39">ArrayAllocationExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"new" "int" "[" <A HREF="#prod12">Expression</A> "]"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod40">AllocationExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"new" <A HREF="#prod4">Identifier</A> "(" ")"</TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod41">NotExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"!" <A HREF="#prod12">Expression</A></TD>
</TR>
<TR>
<TD ALIGN=RIGHT VALIGN=BASELINE><A NAME="prod42">BracketExpression</A></TD>
<TD ALIGN=CENTER VALIGN=BASELINE>::=</TD>
<TD ALIGN=LEFT VALIGN=BASELINE>"(" <A HREF="#prod12">Expression</A> ")"</TD>
</TR>
</TABLE>

The parser is generated using the JavaCC toolchain. Double Dispatching is used to interface
with the generated syntax tree nodes. 