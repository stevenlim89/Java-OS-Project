#include "syscall.h"

int
main (int argc, char *argv[])
{
	char *file = "test.txt";
	int test = creat(file);
	test = unlink(file);

	printf("Test should be zero:   %d", test);
	return 0;
}
