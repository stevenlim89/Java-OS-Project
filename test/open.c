#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int
main (int argc, char *argv[])
{
    char *param = "hello.txt";
    int test = open(param);
    assert(-1 != test); 

    return 0;
}
