#include "syscall.h"

int pid;

int main(int argc,char **argv){
	pid = exec("cat.coff",argc,argv);	
	return 0;
}
