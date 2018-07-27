# BachMeer Syntaxes

This document explains the syntax of BachMeer

## Literals

Integer

```
0xCAFE_DEAD
123 456 789
```

Decimal

```
3.141592
5.
```

String

```
'Yeah'
"It's String"
```

Template String

```
`Yeah It's ${3 + 3}th string`
```

Array

```
[1, 2, 3, 4]
```

Map

```
{a: 1, b: 2, c: 3}
```

## Variable

```
let immutable = 4;
let mut mutable = 6;
mutable = 88;
```

## Function

Declaration

```
func add(i: int, j: int) -> int {
    return i + j
}

func mul(i, j) = i * j
```

Vararg
```
func sum(vararg args) {
    let mut result = args[0]
    for arg in args[1:] {
        result += arg
    }
    return result
}
```

Call

```
add(10, 50)          // 60
add(15., 4)          // Fail, 15. is decimal
add('a', 4)          // Fail, 'a' is string
mul(4, 5)            // 20
mul('a', 4)          // 'aaaa'
mul(2.5, 2)          // 5.0
sum([1, 2, 3])       // 6, vararg can replace to array
sum('a', 'b', 'c')   // 'abc'
```

## Type

```
// contextual type Self(Refers itself type)
type Addable {
    operator func add(other: Self) -> Self
}

func <T : Addable> add(a: T, b: T) = a + b
```

## Pipeline

```
[10, 20, 30] |> sum // 60
```

## Comment

```
// line comment
/*
 * Block
 * Comment
 */
```

## Loop

```
loop {
    /*
     * equals to
       ```
       while(true) {

       }
       ```
     */
}

loop 10 {
    /*
     * equals to
       ```
       let mut counter = 0;
       while(counter-- > 0) {

       }
       ```
     */
}

while(condition) {

}

for i in iterable {

}
```

## If

```
if condition {

} else if condition {

} else {

}
```

## Match

```
match obj {
    is String -> println(obj[::]) // Smart cast
    "data" -> println("Why you data")
    else -> println("lol")
}
