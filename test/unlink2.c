#include "syscall.h"

int
main (int argc, char *argv[])
{
	char *file = "file.txt";	
	int test = creat(file);
	test = open(file);
	test = unlink(file);

	printf("Should not work and be negative one:   %d", test);

	return 0;
}
