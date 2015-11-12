#include "syscall.h"

int
main (int argc, char *argv[])
{
	char *file = "work.txt";	
	/*int test = creat(file);
	printf("Test at creat: %d\n", test);
	test = open(file);
	printf("Test at open: %d\n", test);*/
	int test = unlink(file);

	printf("Should not work and be negative one:   %d", test);

	return 0;
}
