#include "syscall.h"

int 
main (int argc, char *argv[])
{
	int x = exec("write0.coff", argc, 0);
	if( x < 0) { 
		printf("failed"); 
		exit(-1);
	}
	else{ 
		printf("PASSED");
	}

return 0;
}
