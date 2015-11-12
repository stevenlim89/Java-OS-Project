#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int
main (int argc, char *argv[])
{

    char *param = "hello.txt";
    int test = creat(param);
    printf("The value of the creat return is:   %d \n", test);
    test = open(param);
    printf("The value of open return variable is:  %d \n", test);
    return 0;

}
