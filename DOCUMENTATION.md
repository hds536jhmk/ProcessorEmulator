
![](./logo.png)

# Documentation

 - [Command Line Arguments](#command-line-arguments)
   * [--help](#--help)
   * [--version](#--version)
   * [--run](#--run)
   * [--verify](#--verify)
   * [--obfuscate](#--obfuscate)
   * [--command-line](#--command-line)
   * [--skip-warning](#--skip-warning)
   * [--no-config-auto-save](#--no-config-auto-save)
   * [--bits](#--bits)
   * [--memory-size](#--memory-size)
   * [--clock-frequency](#--clock-frequency)
   * [--program](#--program)
   * [--plugin](#--plugin)
   * [--language](#--language)
 - [Common Mistakes](#common-mistakes)
   * [Running on the command line](#running-on-the-command-line)
   * [Declaring variables between instructions](#declaring-variables-between-instructions)
   * [Dividing by ZERO](#dividing-by-zero)
   * [Setting the clock too high](#setting-the-clock-too-high)
 - [Compiler Basics](#compiler-basics)
   * [Constants](#constants)
   * [Labels](#labels)
   * [Compiler Instructions](#compiler-instructions)
   * [Registers and Flags](#registers-and-flags)
 - [Instructions](#instructions)
   * [NULL](#null)
   * [BRK](#brk)
   * [DATA](#data)
   * [MOV](#mov)
   * [SWP](#swp)
   * [XMOV](#xmov)
   * [OUTI](#outi)
   * [OUTC](#outc)
   * [GETI](#geti)
   * [GETC](#getc)
   * [GETK](#getk)
   * [TS](#ts)
   * [TMS](#tms)
   * [INC](#inc)
   * [DEC](#dec)
   * [ADD](#add)
   * [SUB](#sub)
   * [MUL](#mul)
   * [DIV](#div)
   * [MOD](#mod)
   * [AND](#and)
   * [OR](#or)
   * [NOT](#not)
   * [XOR](#xor)
   * [SHL](#shl)
   * [SHR](#shr)
   * [ROL](#rol)
   * [ROR](#ror)
   * [CMP](#cmp)
   * [JMP](#jmp)
   * [JC](#jc)
   * [JNC](#jnc)
   * [JZ](#jz)
   * [JNZ](#jnz)
   * [JE](#je)
   * [JNE](#jne)
   * [JB](#jb)
   * [JNB](#jnb)
   * [JBE](#jbe)
   * [JNBE](#jnbe)
   * [JA](#ja)
   * [JNA](#jna)
   * [JAE](#jae)
   * [JNAE](#jnae)
   * [CALL](#call)
   * [RET](#ret)
   * [PUSH](#push)
   * [POP](#pop)
   * [LOOP](#loop)
   * [HLT](#hlt)

# Command Line Arguments

**NOTE:** I may have forgotten some arguments, so to be sure there's the [--help](#--help) flag which will show all
 possible command line arguments that can be used.

## --help

Type: `Flag`

Short: `-h`

If specified, this flag will show you all the valid command line options, their type and their range if they have one.

## --version

Type: `Flag`

Short: `-ver`

If specified, this flag will print PEMU's version to the console.

## --run

Type: `Flag`

Short: `-r`

If specified, this flag will automatically run the specified program at the Application's start.

It's more useful if used with the [--command-line](#--command-line) flag.

## --verify

Type: `Flag`

Short: `-v`

If specified, this flag will automatically verify the specified program at the Application's start.

It's more useful if used with the [--command-line](#--command-line) flag.

## --obfuscate

Type: `Flag`

Short: `-o`

If specified, this flag will automatically obfuscate the specified program at the Application's start.

It's more useful if used with the [--command-line](#--command-line) flag.

## --command-line

Type: `Flag`

Short: `-cl`

If specified, this will run the Application on the command line.

This is an experimental feature, because everything takes
for granted that it's running on the full app.

Either the [--run](#--run) or the [--verify](#--verify) flag must be specified when using this one.

See Also [Common Mistakes](#common-mistakes) -> [Running on the command line](#running-on-the-command-line).

## --skip-warning

Type: `Flag`

Short: `-sw`

If specified, this flag will skip the warning message generated from the [--command-line](#--command-line) flag.

## --no-config-auto-save

Type: `Flag`

Short: `-ncas`

If specified, this flag will prevent PEMU from saving its config automatically.

It's more useful if used with the [--verify](#--verify) or [--obfuscate](#--obfuscate) flag.

## --bits

Type: `Integer`

Short: `-b`

If specified, the following argument will be treated as an Integer and will be used as the Processor's Word Size.

## --memory-size

Type: `Integer`

Short: `-ms`

If specified, the following argument will be treated as an Integer and will be used as the Processor's Memory Size.

## --clock-frequency

Type: `Integer`

Short: `-cf`

If specified, the following argument will be treated as an Integer and will be used as the Processor's Clock.

## --program

Type: `String`

Short: `-p`

If specified, the following argument will be used to open the program it points to.

## --plugin

Type: `String`

Short: `-pl`

If specified, the following argument will be used as the ID of the plugin to load.

## --language

Type: `String`

Short: `-lang`

If specified, the following argument will be used as the short name of the language to use.

# Common Mistakes

## Running on the command line

While it's so cool to be able to run everything on the command line there are some drawbacks that I'm never even going
to attempt to fix, because they are what made me create a Swing application in the first place:

 - There's no easy way of getting key strokes
 - There's no easy Platform-Independent way of clearing the command line

Other issues include:

 - [Breakpoints](#brk) are actual Instructions that pause the Processor, because there's no way of resuming its
    execution through the command line then breakpoints will break (pun intended)
 - No debugging features

Those are the main issues with running programs on your beautiful command line. So don't expect all programs to work
properly on it, if something feels wrong then just run it on the full app.

## Declaring variables between instructions

The following example would show the variable to the console and execute the instruction with code 20 and stop
the processor:

```Assembly
OUTI var
var: #DW 20
HLT
```

We don't want to execute that because it's not intended to be an instruction, a fix for that would be adding a label
which indicates where the program starts and declaring variables before that:

```Assembly
JMP start

var: #DW 20

start:
OUTI var
HLT
```

The code above won't execute the contents of the declared variable.

## Dividing by ZERO

**NOTE:** Since 1.3.X the error should be more clear, an `InstructionError` exception should be thrown describing which
instruction gave that error with its address.

The processor crashes when dividing a number by zero, no division by zero is executed inside the emulator's code,
so most of the time it's the user's fault.

## Setting the clock too high

The clock can go up to 1 instruction every nanosecond (1GHz), though it's overkill for most programs and will probably
lock the emulator if the program has an uncapped loop that writes to the console, so it's not recommended going above the
1kHz mark except for programs that have a cap on their draw loop.

# Compiler Basics

## Constants

Constants can be declared as follows:

```Assembly
@const_variable 10
```

Constants are values, they don't point to anything in memory, so they are rarely used as arguments for instructions (see [DATA](#data)).

Though they are still useful if used to specify User Settings at the top of the source file or within Libraries as
"Library Settings".

```Assembly
; Since this doesn't change memory it can be put at the top of
;  the program's source (And no random instruction will be executed)
;  to let the user find it and change it more easily
@time_till_stop 10

JMP start

start:
HLT

; We can add it to memory where we like
key: #DW @time_till_stop
```

```Assembly
HLT

; The library file to be able to compile must still specify
;  the constant before using it
#INCLUDE "mylib.pemulib"

; But it can be changed/used by others
@MY_LIB_SETTING @VK_ENTER
```

Values that can be assigned to constants are: Previously Declared Constants, Characters or Numbers

```Assembly
@key_confirm @VK_ENTER
@char_comma ','
@n_delay 500
```

A Constant's value is the very last one that was assigned to it.

Though if used as an array's length or offset their value will be the one that was assigned before said array/offset
creation.

There are some constants that are already declared, such as virtual keys (or VK), the emulator uses reflection to get
all VKs from the [KeyEvent](https://docs.oracle.com/javase/7/docs/api/java/awt/event/KeyEvent.html) class that then are
used as a base template for constants. So if you want to declare a variable that stores the value of a virtual key you
can go to the above linked class's docs and search if there's the field (starting with `VK_`) that you want to use:

```Assembly
; If I want to put the enter key's key code inside my memory,
;  I can do it like this:
VK_ENTER: #DW @VK_ENTER
; Then I can use the declared variable to check if the key was pressed
```

## Labels

These are the most useful, since they point to somewhere in memory, where they point is based on where they are declared:

```Assembly
; Since labels can only be declared once, they are global to the program
; All instructions use pointers to handle arguments
; i.e. OUTC wants the pointer to the character to print
OUTC char

; This label will always point to the memory where
;  the character 'C' is stored
char: #DW 'C'
```

Labels are declared by putting `:` in front of a name and used by their name, because they aren't declared and
used using the same syntax, if the compiler sees `:` after a name, it will always try to turn it into a label:

```Assembly
; Moving the contents of str_ptr to arg
; This makes possible to change instruction arguments at runtime
MOV arg str_ptr
OUTC arg: 0
HLT

; Declaring a label that points to a string
str: #DS "Hello World!\0"
; Declaring a label that points to a label's address
str_ptr: #DW str
```

An offset can also be specified on labels using the characters `[` and `]` and putting either a constant
or static number between them:

```Assembly
@offset 0

; This will print the character at the address specified
;  by str + the specified offset (that can be either a constant or number)
OUTC str[ @offset ]
OUTC str[11]
HLT

; Declaring a label that points to a string
str: #DS "Hello World!\0"
```

## Compiler Instructions

These instructions are useful to put values into memory where there's no processor instruction:

```Assembly
; This puts the value 10 at address 0
#DW 10
```

They are 4 and always have an `#` in front of them:
 - \#DW (Define Word): Can be followed by a [constant](#constants), a [label](#labels), a character (`'\''`) or a numeric value.
 - \#DS (Define String): Can only be followed by a string (`"\"This is a string\""` or `'"This is a string"'`).
 - \#DA (Define Array): Can be followed by either an array (`{ 3 2 newline: '\n' @VK_ENTER }`) where said array can contain
   constants, labels, characters, offsets or numbers, or an array size (`[10]`).
 - \#INCLUDE: Can only be followed by a String and said String must point to a File using a Path relative to the File
   where the instruction is found.

```Assembly
; This declares a label called "number" that points to
;  the address 0, which contains the value defined by #DW, which is 30
number: #DW 30

; Characters can also be defined using #DW:
character: #DW '\n'

; #DS adds all characters in the string to memory, so the label "string"
;  is only pointing to the first character ("H")
string: #DS "Hello World!\0"

; Strings and Characters support common special characters: '\n', '\t'
;   and Code Points: '\0', '\10', '\13'
; On Strings multiple Code Points can be defined: "\10\0"
;   If you want to add a digit after a Code Point, it can be terminated using a semicolon (;):
;   "\10;2\0"

; #DA adds all elements in the array to memory, constants can be used too
;  and labels can be declared pointing to a certain element
array: #DA {
   10 3 2
   ; Comments can also be used within arrays to comment your elements
   enter_key: @VK_ENTER
}
; This is useful to define multiple words without using multiple define
;  word instructions, Note that no string can be defined within an Array

; If you just want to reserve Memory for an Array you can do
prealloc_array: #DA [10]
; Where 10 is the size of the Array

; By convention all library files have the extension ".pemulib"
; The path is relative to the file that's including the library
#INCLUDE "stdlib/print.pemulib"
; Note that INCLUDE instructions should only be added after the main function call/jump
;  because they add the library where the instruction was specified so random instructions
;  may be executed otherwise, that's why an HLT instruction should be added at the top of your library code
; Also if the same file is INCLUDED twice, then the second INCLUDE will be ignored
```

## Registers and Flags

If the specified Register/Flag supports it, Registers/Flags can be used in the same places as [Labels](#labels).

For Example: They're useful if you want to get the location of the Stack:

```Assembly
; This label points at the Stack Pointer Register's address
SP_ptr: #DW SP
```

If the specified Register/Flag doesn't support this type of operation
(aka The Register/Flag isn't an instance of MemoryRegister/MemoryFlag),
then an error will be thrown on the Debug Console.

**NOTE:** Multiple Flags can be stored at the same address, so it isn't that useful to reference Flags
unless you're certain about the bit that represents the Flag you want to get.

# Instructions

## NULL

This instruction doesn't do anything and its code is 0, this is used to ignore empty memory.

## BRK

This instruction is a simple breakpoint, the processor can be resumed by going to Processor->Pause/Resume.
This instruction shouldn't be used in the final program, it's just a way of debugging your code by automatically pausing
the processor at a point in the program without having to press the pause button in the app.

If you're running on the Command Line then see
[Common Mistakes](#common-mistakes) -> [Running on the command line](#running-on-the-command-line).

## DATA

`DATA addr val`

Sets the value at `addr` to `val`.

**Example:**

```Assembly
; Sets the contents of val to the value of the
;  constant @VK_A
DATA val @VK_A

; Printing contents of val, it isn't equal to
;  10 because it was overridden by DATA
OUTI val
HLT

val: #DW 10
```

## MOV

`MOV dst src`

Moves the contents of `src` into `dst` (doesn't reset `src`).

**Example:**

```Assembly
; This is used to show on the console
;  what values the two variables hold
OUTI _value1 OUTC space OUTI _value2

; This copies the value at _value2
;  to _value1, so _value1 becomes 25
MOV _value1 _value2

OUTI _value1 OUTC space OUTI _value2
HLT

_value1: #DW 0
_value2: #DW 25
space: #DW ' '
```

## SWP

`SWP val1 val2`

Swaps the contents of `val1` and `val2`.

**Example:**

```Assembly
; This is used to show on the console
;  what values the two variables hold
OUTI _value1 OUTC space OUTI _value2

; This swaps the values in _value1 and _value2
;  this makes _value1 become 25 and _value2 0
SWP _value1 _value2

OUTI _value1 OUTC space OUTI _value2
HLT

_value1: #DW 0
_value2: #DW 25
space: #DW ' '
```

## XMOV

`XMOV dst src opt`

This moves `src` to `dst` using `opt` that describes how to move the contents of `src` to `dst`.

The first 3 bits of `opt` are used as flags by the instruction to know how to perform the movement, `0bXSD`:
 - `D` says whether to use the contents of `dst` as the address of the destination
 - `S` says whether to use the contents of `src` as the address of the source
 - `X` says whether to swap `src` and `dst`

**Example:**

```Assembly
; This example is best viewed by looking at
;  the Memory using the MemoryView panel (Ctrl + M)
; And by stepping through the instructions
;  using Processor->Step (Shift + S)

BRK
; This moves to val1 the contents of src
XMOV val1 src 0b000
; This swaps the contents of whatever dst
;  and src's contents are pointing to
XMOV dst  src 0b111

HLT

dst: #DW val1
src: #DW val2

val1: #DW 3
val2: #DW 5
```

## OUTI

`OUTI val`

Outputs the contents of `val` to the console as a number.

**Example:**

```Assembly
; This prints on the console the
;  specified variable as a number
OUTI _number

HLT

_number: #DW 136
```

## OUTC

`OUTC char`

Outputs the contents of `char` to the console as a character.
The null termination character can be used to clear the console.

**Example:**

```Assembly
; This prints on the console the
;  specified variable as a character
; If the null character is displayed
;  the console is cleared
OUTC _char

HLT

_char: #DW 'C'
```

## GETI

`GETI dst`

Gets the currently selected key's numeric value and sets `dst` to it.
If the pressed key doesn't have a numeric character, it's set to **0**.

**Example:**

```Assembly
loop:

; Gets the currently pressed key
;  converted to a number and stores
;  it in the specified variable
GETI _number_pressed

; Clearing console and displaying
;  the pressed number
OUTC null
OUTI _number_pressed

; Checking if the number pressed is
;  the one used to exit the loop
CMP _number_pressed _quit_value
JNZ loop
HLT

_number_pressed: #DW 0
_quit_value: #DW 9
null: #DW '\0'
```

## GETC

`GETC dst`

Gets the currently selected valid character and sets `dst` to its [char code](https://www.rapidtables.com/code/text/ascii-table.html).
If no valid character is pressed, it's set to **0**.

**Example:**

```Assembly
loop:

; Gets the currently pressed
;  valid character
GETC _char_pressed

; Clearing console and displaying
;  the pressed character
OUTC null
OUTI _char_pressed

; Checking if the char pressed is
;  the one used to exit the loop
CMP _char_pressed _quit_char
JNZ loop
HLT

_char_pressed: #DW 0
_quit_char: #DW 'q'
null: #DW 0
```

## GETK

`GETK dst`

Gets the currently pressed key and sets `dst` to its [keycode](https://docs.oracle.com/javase/7/docs/api/java/awt/event/KeyEvent.html)
(Constants can be used to add keys to memory. e.g. `@VK_ENTER`).
If no key is pressed, it's set to the constant `@VK_UNDEFINED`.

**Example:**

```Assembly
loop:

; Keycodes can be found @https://stackoverflow.com/a/31637206
; Gets the currently pressed key
GETK _key_pressed

; Clearing console and displaying
;  the pressed key keycode
OUTC null
OUTI _key_pressed

; Checking if the key pressed is
;  the ENTER key
CMP _key_pressed VK_ENTER
JNZ loop
HLT

_key_pressed: #DW 0
VK_ENTER: #DW @VK_ENTER
null: #DW 0
```

## TS

`TS dst`

Gets the seconds elapsed since the processor started and puts them into `dst`.

**Example:**

```Assembly
loop:

; Getting time elapsed (in seconds) since the
;  processor started, using a max Clock of 1kHz is recommended
TS _time_elapsed

; Showing the time on the console
OUTC null
OUTI _time_elapsed

; Stopping processor after 10 seconds
CMP _time_elapsed _max_time
JC loop
HLT

_time_elapsed: #DW 0
_max_time: #DW 10
null: #DW 0
```

## TMS

`TMS dst`

Gets the milliseconds elapsed since the processor started and puts them into `dst`.
**This instruction is basically useless if using a processor with words that are less than 16 bits long.**

**Example:**

```Assembly
; This example only works with processors that have
;  16 bits or greater words, using a max Clock of 1kHz is recommended
loop:

; Getting time elapsed (in milliseconds) since the
;  processor started
TMS _time_elapsed

; Showing the time on the console
OUTC null
OUTI _time_elapsed

; Stopping processor after 10 seconds
CMP _time_elapsed _max_time
JC loop
HLT

_time_elapsed: #DW 0
_max_time: #DW 10000
null: #DW '\0'
```

## INC

`INC target`

Increments the value of `target` by 1.
Sets Carry and Zero flags accordingly.

**Example:**

```Assembly
; Showing value on the console
OUTI _value

; Incrementing _value
INC _value

OUTC newline
OUTI _value
HLT

_value: #DW 10
newline: #DW '\n'
```

## DEC

`DEC target`

Decrements the value of `target` by 1.
Sets Carry and Zero flags accordingly.

**Example:**

```Assembly
; Showing value on the console
OUTI _value

; Decrementing _value
DEC _value

OUTC newline
OUTI _value
HLT

_value: #DW 10
newline: #DW '\n'
```

## ADD

`ADD a b`

Adds `b` to `a` and stores the result in `a`.
Sets Carry and Zero flags accordingly.

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC plus_sign OUTI _b OUTC equal_sign

; Adding _b to _a and storing value in _a
ADD _a _b

OUTI _a
HLT

_a: #DW 11
_b: #DW 10
plus_sign: #DW '+'
equal_sign: #DW '='
```

## SUB

`SUB a b`

Subtracts `b` from `a` and stores the result in `a`.
Sets Carry and Zero flags accordingly.

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC minus_sign OUTI _b OUTC equal_sign

; Subtracting _b to _a and storing value in _a
SUB _a _b

OUTI _a
HLT

_a: #DW 11
_b: #DW 10
minus_sign: #DW '-'
equal_sign: #DW '='
```

## MUL

`MUL a b`

Multiplies `a` by `b` and stores the result in `a`.
Sets Carry and Zero flags accordingly.

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC mult_sign OUTI _b OUTC equal_sign

; Multiplying _a by _b and storing value in _a
MUL _a _b

OUTI _a
HLT

_a: #DW 11
_b: #DW 10
mult_sign: #DW '*'
equal_sign: #DW '='
```

## DIV

`DIV a b`

Divides `a` by `b` and stores the result (floored) in `a`.
Sets Carry and Zero flags accordingly.

**NOTE:** May produce Division by Zero exception.

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC div_sign OUTI _b OUTC equal_sign

; Dividing _a by _b and storing value in _a
; NOTE: The result is floored, division by 0 will cause
;        the processor to crash
DIV _a _b

OUTI _a
HLT

_a: #DW 11
_b: #DW 10
div_sign: #DW '/'
equal_sign: #DW '='
```

## MOD

`MOD a b`

Stores `a` modulus `b` in `a`.
Sets Carry and Zero flags accordingly.

**NOTE:** May produce Division by Zero exception.

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC modulo_sign OUTI _b OUTC equal_sign

; Calculating _a mod _b and storing value in _a
MOD _a _b

OUTI _a
HLT

_a: #DW 11
_b: #DW 3
modulo_sign: #DW '%'
equal_sign: #DW '='
```

## AND

`AND a b`

Calculates the Bitwise And on `a` and `b` and stores the result in `a`.
Sets Zero flag accordingly (Carry flag doesn't change).

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC and_sign OUTI _b OUTC equal_sign

; Calculating _a & _b and storing value in _a
AND _a _b

OUTI _a
HLT

_a: #DW 0x0F
_b: #DW 0xFA
and_sign: #DW '&'
equal_sign: #DW '='
```

## OR

`OR a b`

Calculates the Bitwise Or on `a` and `b` and stores the result in `a`.
Sets Zero flag accordingly (Carry flag doesn't change).

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC or_sign OUTI _b OUTC equal_sign

; Calculating _a | _b and storing value in _a
OR _a _b

OUTI _a
HLT

_a: #DW 0x0C
_b: #DW 0xF2
or_sign: #DW '|'
equal_sign: #DW '='
```

## NOT

`NOT val`

Calculates the Bitwise Not on `val` and stores the result in `val`.
Sets Zero flag accordingly (Carry flag doesn't change).

**Example:**

```Assembly
; Showing operation on the console
OUTC not_sign OUTI _val OUTC equal_sign

; Calculating ~_val storing value in _val
NOT _val

OUTI _val
HLT

_val: #DW 0x0F
not_sign: #DW '~'
equal_sign: #DW '='
```

## XOR

`XOR a b`

Calculates the Bitwise Xor on `a` and `b` and stores the result in `a`.
Sets Zero flag accordingly (Carry flag doesn't change).

**Example:**

```Assembly
; Showing operation on the console
OUTI _a OUTC xor_sign OUTI _b OUTC equal_sign

; Calculating _a ^ _b and storing value in _a
XOR _a _b

OUTI _a
HLT

_a: #DW 0x0C
_b: #DW 0xAD
xor_sign: #DW '^'
equal_sign: #DW '='
```

## SHL

`SHL a b`

Shifts left `a` by `b` and stores the result in `a`.
Sets Carry flag to the last bit that fell off (Zero flag doesn't change).

```Assembly
; Given an 8 bit Processor
; And the binary num:
;  0b11010010
; The result will be (If shifted by 1):
;  0b10100100
; With CF = 1
; If no shift occurs CF = 0
SHL num shift
HLT

num: #DW 0b11010010
shift: #DW 1
```

## SHR

`SHR a b`

Shifts right `a` by `b` and stores the result in `a`.
Sets Carry flag to the last bit that fell off (Zero flag doesn't change).

```Assembly
; Given an 8 bit Processor
; And the binary num:
;  0b01000100
; The result will be (If shifted by 1):
;  0b00100010
; With CF = 0
; If no shift occurs CF = 0
SHR num shift
HLT

num: #DW 0b01000100
shift: #DW 1
```

## ROL

`ROL a b`

Rotates left `a` by `b` and stores the result in `a`.
Sets Carry flag to the Least Significant Bit of the result (Zero flag doesn't change).

```Assembly
; Given an 8 bit Processor
; And the binary num:
;  0b01010001
; The result will be (If rotated by 1):
;  0b10100010
; With CF = 0
; If no rotation occurs CF = 0
ROL num rot
HLT

num: #DW 0b01010001
rot: #DW 1
```

## ROR

`ROR a b`

Rotates right `a` by `b` and stores the result in `a`.
Sets Carry flag to the Most Significant Bit of the result (Zero flag doesn't change).

```Assembly
; Given an 8 bit Processor
; And the binary num:
;  0b10010011
; The result will be:
;  0b11001001
; With CF = 1
; If no rotation occurs CF = 0
ROR num rot
HLT

num: #DW 0b10010011
rot: #DW 1
```

## CMP

`CMP a b`

Compares `a` and `b` and sets Carry and Zero flags as follows:
 - Carry: `a < b`
 - Zero: `a == b`

**Example:**

```Assembly
; Jump to the start of the program
JMP start

; Temp registry, used to store temp values
_temp: #DW 0

; Function's arguments
_are_eq_a: #DW 0
_are_eq_b: #DW 0
; Function that prints if two numbers are equal
f_are_eq:
    ; Getting arguments
    ; (When calling a function the first thing on the stack is the
    ;  return pointer, so we need to POP it and PUSH it again)
    POP _temp
    POP _are_eq_a
    POP _are_eq_b
    PUSH _temp

    ; Compare the two arguments and set
    ;  Zero and Carry flags accordingly
    ;  (Zero = 1 -> a == b; Carry = 1 -> a < b)
    CMP _are_eq_a _are_eq_b
    ; If Zero flag is off (not equal), jump to the
    ;  part of the function that prints that
    JNZ _are_eq_not

    ; If they are equal print that and return
    OUTI _are_eq_a OUTC equal_sign OUTC equal_sign OUTI _are_eq_b
    RET

    _are_eq_not:
    OUTI _are_eq_a OUTC not_sign OUTC equal_sign OUTI _are_eq_b
    RET

start:
    ; Pushing arguments to the stack
    ;  and calling the function
    PUSH _v1 PUSH _v2
    CALL f_are_eq

    OUTC newline

    PUSH _v1 PUSH _v3
    CALL f_are_eq
    HLT

_v1: #DW 11
_v2: #DW 10
_v3: #DW 11

equal_sign: #DW '='
not_sign: #DW '!'
newline: #DW '\n'
```

## JMP

`JMP addr`

Jumps to the specified address (`addr`).

**Example:**

```Assembly
; Jumping to the start label
JMP start

; This instruction doesn't get executed because it was jumped
OUTC _char

start:
; Jumping to the end label
JMP end

; This instruction doesn't get executed because it was jumped
OUTC _char

end:
HLT

_char: #DW 'H'
```

## JC

The same as [JMP](#jmp) but gets executed only if the Carry flag is set to true.

## JNC

The same as [JMP](#jmp) but gets executed only if the Carry flag is set to false.

## JZ

The same as [JMP](#jmp) but gets executed only if the Zero flag is set to true.

## JNZ

The same as [JMP](#jmp) but gets executed only if the Zero flag is set to false.

## JE

The same as [JMP](#jmp) but gets executed only if the last two compared numbers were equal.

## JNE

The same as [JMP](#jmp) but gets executed only if the last two compared numbers were not equal.

## JB

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers was below the second one.

## JNB

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers wasn't below the second one.

## JBE

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers was below or equal to
the second one.

## JNBE

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers wasn't below or equal to
the second one.

## JA

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers was above the second one.

## JNA

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers wasn't above the second one.

## JAE

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers was above or equal to
the second one.

## JNAE

The same as [JMP](#jmp) but gets executed only if the first of the last two compared numbers wasn't above or equal to
the second one.

## CALL

`CALL addr`

Jumps to the specified address (`addr`) and pushes the pointer to the next instruction into the stack.
This is used with [RET](#ret).

**Example:**

```Assembly
JMP start

_temp: #DW 0

_inc_max_value_ptr: #DW 0
_inc_max_max_value: #DW 0
; Declaring function that will get called
f_inc_max:
    ; Popping arguments from the stack without forgetting that
    ;  there's also the return pointer because this is a function
    POP _temp
    POP _inc_max_max_value
    POP _inc_max_value_ptr
    PUSH _temp
    
    ; Moving the pointer to the value to CMP's first argument
    MOV _inc_max_cmp_ptr _inc_max_value_ptr
    CMP _inc_max_cmp_ptr: 0 _inc_max_max_value
    JZ _inc_max_end
    
    ; Moving the pointer to the value to INC's first argument
    MOV _inc_max_inc_ptr _inc_max_value_ptr
    INC _inc_max_inc_ptr: 0
    
    _inc_max_end:
    ; Use the return pointer in the stack to go back
    ;  to where this function was called
    RET

start:
; Print to the console the value
OUTI _value
OUTC newline

; Pushing arguments for function
;  and using CALL to call it
PUSH _value_ptr PUSH _max
CALL f_inc_max ; The value should be 11

PUSH _value_ptr PUSH _max
CALL f_inc_max ; The value should be 12

PUSH _value_ptr PUSH _max
CALL f_inc_max ; The value should still be 12

OUTI _value
HLT

_value: #DW 10
_value_ptr: #DW _value
_max: #DW 12
newline: #DW '\n'
```

## RET

`RET`

Pops the stack and jumps to the address that was popped.
This is used with [CALL](#call), see its example.

## PUSH

`PUSH val`

Pushes `val` to the stack.
This is used with [POP](#pop).

**Example:**

```Assembly
; Printing the value of _v1 to the console
OUTI _v1
OUTC newline

; Pushing the value of _v1 to the stack
PUSH _v1
; Popping the last value on the stack to
;  the variable _v2
POP _v2

; Printing the value of _v2 to the console
;  (Should be the same as _v1)
OUTI _v2
HLT

_v1: #DW 33
_v2: #DW 46
newline: #DW '\n'
```

## POP

`POP dst`

Pops the last value on the stack and stores it into `dst`.
This is used with [PUSH](#push), see its example.

## LOOP

`LOOP jmpdst countaddr`

Decrements by 1 the number at `countaddr` and jumps to `jmpdst` if it's not equal to 0.

**NOTE**: If the value at `countaddr` is 0 then it will wrap around to `2^WordSize`.

**Example:**

```Assembly
; Setting the contents of _count to 10
DATA _count 10
; Declaring the label that points to the body
;  of the loop
loop:
   ; Printing out the count
   OUTI _count
   OUTC newline
; Looping if _count - 1 isn't equal to 0
LOOP loop _count
HLT

_count: #DW 0
newline: #DW '\n'
```

The example above can be seen as the following Java code:

```Java
int _count = 10;
do {
    System.out.println(_count);
} while (--_count != 0);
```

## HLT

`HLT`

Tells the processor to stop the execution of the program.

**Example:**

```Assembly
start:
; Tells the processor to stop the execution
;  of this program
HLT

; This instruction will never get executed
JMP start
```
