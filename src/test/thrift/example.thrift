namespace java ai.diffy.thriftjava
#@namespace scala ai.diffy.thriftscala

service Adder {
    i32 add(
        1: i32 a,
        2: i32 b
    )
}