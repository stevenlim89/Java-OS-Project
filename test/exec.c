#include "syscall.h"

int 
main (int argc, char *argv[])
{
	int x = exec("write0.coff", argc, 0);
	printf("This is the return value:  %d", x);
	return 0;
}
