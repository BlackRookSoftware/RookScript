# Rookscript Language Quick Guide


### Comments

```
	// This is a single-line comment.
	/*
		This is a multi
		line comment.
	*/
	/* You can even write them like this. */
```

All comments are ignored, and are not preserved after compilation.


### Value Types

Booleans

```
	true
	false
```

Integers (64-bit)

```
	0
	5
	34
	0xff		// hexadecimal notation
	0X0ff		// hexadecimal notation
	0x6767		// hexadecimal notation
	0x00fd34dd	// hexadecimal notation
```

Floating-point (64-bit)

```
	0.0
	0.456
	1.467
	1e2		    // exponent notation
	0.5e6		// exponent notation
	1.545E7		// exponent notation
	4.605e-7	// exponent notation
	0.44444e+3	// exponent notation
```

Strings (wrapped in double quotes)

```
	""
	"apple"
	"104"
	"Matt Tropiano"
	"abracadabra"
```

Lists

```
	[]			// empty list
	[5]			// one element
	[1, 2, 3]
	["apple", "orange", "pear", "kumquat"]
	[1, 1.0, "apple", false, null]			// can hold multiple types
	[[1,2,3],[4,5,6]]						// can hold nested lists
```

Maps

```
	{}				// empty map
	{x: 5, y: 6}	// map with two fields
	{				// map with several fields and types (values can be any type)
		name: "Bob", 
		age: 23,
		gender: "Male",
		likes: ["Golf", "Baseball"],
		dislikes: ["Writing documentation", "Cold weather", "mean people"],
		rating: 9.5
	}
```


**Objects** have no literal representation, but are storable as variables.

There are some special constants used as well:

```
	null		// object, represents "nothing."
	infinity	// floating-point, IEEE positive infinity
	NaN			// floating-point, IEEE not-a-number ("nan" also works - it is case-insensitive)
```


#### Logical Equivalence

Some parts of Rookscript evaluate values logically. For those cases, the following values are considered `false`:

```
    null
    false
    0
    0.0
    NaN
    ""      // empty string
    []      // empty list
	{}		// empty map
```

Everything else is considered `true`.


### Variables

All variables/identifiers must not start with a number (since numbers do), and must be comprised
of alphanumeric characters plus the underscore (_).

```
	x
	arg0
	VARNAME
	this_is_a_variable_too
```

List contents are read via 0-based index:

```
    x = [4,5,6,7]
    x[0]            //  4
    x[1]            //  5
    x[2]            //  6
    x[3]            //  7
    x[3.5]          //  7 (3.5 is chopped to 3)
    x[-1]           //  null (values outside of a list are null)
```


### Statements

Each script is a series of **statements**. Statements are usually function calls or expressions that assign values to variables. All statements are terminated with a semicolon (`;`).

Some statements look like this:

```
    y = 9;                      // assigns 9 to y
    age = 24;                   // assigns 24 to age
    x = 4.0 + 5.0;              // assigns 9.0 to x
    b = double(4);              // calls the function "double" with 4 as its only parameter, and assigns the result to b.
    tagPerson("Bob", "old");    // calls the function "tagPerson" with "Bob" and "old" as parameters.
    doWork();                   // calls the function "doWork" with no parameters.
```

A *block* of statements, executed in the order written, are placed between *braces*:

```
{
    x = 24;         // assign 24 to x
    y = 9;          // assign 9 to y
    z = x + y;      // assign the result of x + y (33) to z
}
```


### Control Statements

There are a few special types of statements that control execution.


#### The "If" Statement

"If" statements represent a branch in logic - if the expression in the parenthesis evaluates to `true`, the subsequent block is executed.

```
    if (x == 4)
    {
        print("x is 4!");
    }
```

If an "if" statement is followed up by an "else" block, that block will be executed instead.

```
    if (x == 4)
    {
        print("x is 4!");
    }
    else
    {
        print("x is not 4!");
    }
```

A block of statements can have the braces omitted if it is just one statement.

```
    if (x == 4)
        print("x is 4!");
    else
        print("x is not 4!");
```

For this reason, you can chain together if-elses...

```
    if (x == 4)
    {
        print("x is 4!");
    }
    else if (x == 5)
    {
        print("x is 5!");
    }
    else
    {
        print("x is neither 4 nor 5!");
    }
```

...since this is equivalent:

```
    if (x == 4)
    {
        print("x is 4!");
    }
    else 
    {
        if (x == 5)
        {
            print("x is 5!");
        }
        else
        {
            print("x is neither 4 nor 5!");
        }
    }
```

#### The "While" Loop

"While" loops execute the following block of statements as long as the expression in the parenthesis is still `true` when the block completes.

```
    x = 5;
    // This code will loop 5 times.
    while (x > 0)
    {
        print("x is " + x);
        x = x - 1;
    }
```

#### The "For" Loop

"For" loops execute the following block of statements as long as the conditional expression is still `true` when the block completes.

*For* loops have an *initializer*, *conditional*, and *step* part enclosed in parenthesis that drives the loop. Each of these parts are separated by a semicolon (`;`), except for the last part.

```
    // This code will loop 10 times.
    for (x = 0; x < 10; x = x + 1)
    {
        print("x is " + x);
    }
```

Each part can be left out of a *for* loop, except for the conditional. The previous loop can be rewritten as:

```
    x = 10;
    for (; x < 10;)
    {
        print("x is " + x);
        x = x + 1;
    }
```


### Expressions

Expressions are infix-notated (like how people write it) and are comprised of values and operators.

```
	5 + 4
	x - 3 / 6
	9 + -4 - (x * 3.0)
	!false
	"apple" + "sauce"
```

There are **unary** operators:

```
	!       // logical not (!false !4.0)
	~	    // bitwise not (~5)
	-	    // negation	   (-3)
	+	    // absolute    (+(-16) +9.02)
```

and there are **binary** operators:

```
	+	    // addition
	-	    // subtraction
	*	    // multiplication
	/	    // division
	%	    // modulo division
	&	    // bitwise and
	|	    // bitwise or
	^	    // bitwise xor
	==      // logical equals
	===     // logical strict equals (type and value)
	!=      // logical not equal
	!==     // logical strict not equals (type and value)
	<       // logical less than
	>       // logical greater than
	<=      // logical less than or equal
	>=      // logical greater than or equal
	<<      // left bit shift
	>>      // right bit shift
	>>>     // right bit shift (zero-padded)
```

All operators have a defined precedence:

```
    4 + 5 * 3       // equals 19, not 27
```

In order of precedence (same row is same precedence):

```
    ! ~ - +         (all unary operators)
    * / %
    + -
    >> >>> <<
    > >= < <=
    == === != !==
    &
    ^
    |
```

The following binary operators **short-circuit** their logic:

```
    &&      // logical and - short circuit if left side is false
    ||      // logical or - short circuit if left side is true
    ?:      // null coalesce - short circuit if left side is not null, return first non-null.
```

There is also a **ternary** operator:

```
    ? :         // if before the '?' is true-equivalent, evaluate before the colon, else after.
    
    x ? 5 : 7   // returns 5 if x is true-equivalent, else returns 7
```

The aforementioned short-circuiters have **lowest** precedence.

```
    4 + 7 && 3 - 3 && 12.0     // returns false (11 && 0). 0 is false-equivalent. 12.0 is not even evaluated.
```

Precedence can be altered using parenthesis - these essentially create sub-expressions within expressions:

```
    4 + 5 * 3       // equals 19, not 27
    4 + (5 * 3)     // equals 19, not 27
    (4 + 5) * 3     // equals 27, not 19
```

When using operators, some either result in implicit type promotion, or reduction. This is critical to think about for comparisions later!

```
    4 + 5.0         // result is 9.0 - floating-point
    !3              // result is false - boolean
```

### Entry Points

All starting points in a script's execution are called *entry points* and are declared as such:

```
    entry start()
    {
        // ... statements go here
    }
```

...this declares an entry point called `start` that has no parameters.

Entry points can declare any number of parameters, declared as identifiers that hold the values that are passed in when they are called from the host. The `return` keyword returns execution to the host plus a value result, if any.


### Functions

#### Local Functions

*Local Functions* are simple subroutines. They are declared in the script like so:

```
    function name(parameter0, parameter1)
    {
        // ... code
    }
```

Like *entry points*, they can declare any number of parameters, declared as identifiers that hold the values that are passed in when they are called. The `return` keyword returns execution to the caller plus a value result. *All local functions return a value.* If it never hits a `return` statement, they return `null`. **It is good practice to ensure that a function hits a `return` at every branch in execution if it is supposed to return a value!**

Functions, however, **cannot** be the start of a script's execution, like *entry points*.

This is a function that doubles a value and returns it:

```
    function double(x)
    {
        return x * 2;
    }
```

This is a function that calls other functions (and returns `null`):

```
	// print is a void function that prints a message to STDOUT.

    function printUppercase(message)
    {
        u = strupper(message);
        print(u);
    }
```

Functions in Rookscript support *recursion*, or functions that call themselves.

```
    function factorial(x)
    {
        if (x <= 0)
            return 1;
        else
            return x * factorial(x - 1);
    }
```

If a function is called with less parameters than required, the remaining parameters are filled with `null`.

```
	// print is a void function that prints a message to STDOUT.

    function printStuff(p1, p2, p3)
    {
        print("p1 is " + p1);
        print("p2 is " + p2);
        print("p3 is " + p3);
    }
    
    entry main()
    {
        printStuff(1, 2);
        
        /*
            Will print:
            p1 is 1
            p2 is 2
            p3 is null
        */
    }
```

#### Host Functions

*Host Functions* are functions that are provided by the host implementation. They are not declared in the script. They are called like local functions, and are null-filled like local functions.

A host function, however, can be **void**, which indicates it may not return anything at all (or rather, it doesn't push anything back onto the stack). As a result, functions that are **void** cannot be used in expressions.

```
	// print is a void function that prints a message to STDOUT.

    entry main()
    {
		print("Hello");				// works.
		x = print("Hello") + 4;		// ERROR: Does not compile!
    }
```

#### Function Chains

*Function Chains* are function calls that use the return value of one function to be used as the *first parameter* of the next function called. The return of that function can also be used as the first parameter of the next function, etc.

This makes functions that return the object that it operates on more useful, as well as uses that object as the first parameter.

The function chain operator (a.k.a. the "partial application" operator) is `->`. You must also omit the first parameter of the function being called.

```
	function double(i)
	{
		return 2 * i;
	}

	// print is a void function that prints a message to STDOUT.

	entry main()
	{
		x = 2;
		print(x->double()); 			// prints "4"
		print(x->double()->double()); 	// prints "8"
	}
```

You may use literals as the start of the chain, provided that they are enclosed in parenthesis:

```
	function double(i)
	{
		return 2 * i;
	}

	// print is a void function that prints a message to STDOUT.

	entry main()
	{
		print((2)->double()); 			// prints "4"
		print((2)->double()->double()); // prints "8"
	}
```

You may call a **void** host function in chains, however the chain *must stop at the void function*.

```
	function double(i)
	{
		return 2 * i;
	}

	// print is a void function that prints a message to STDOUT.

	entry main()
	{
		x = 2;
		x->double()->print(); 			// prints "4"
		x->double()->print()->double(); // ERROR: Will not compile! print(...) is a void function!
	}
```

It can be really useful!

```
	function vec2(x, y)
	{
		return {x: x, y: y};
	}

	// Adds components to a vector.
	function vec2Add(v2, x, y)
	{
		v2.x += x;
		v2.y += y;
		return v2;
	}

	// Negates a vector.
	function vec2Negate(v2)
	{
		v2.x = -v2.x;
		v2.y = -v2.y;
		return v2;
	}

	// print is a void function that prints a message to STDOUT.

	entry main()
	{
		v = vec2(0, 0);
		v->vec2Add(3, 4)->vec2Negate();
		print(v);		// prints "{x: -3, y: -4}"
	}
```

