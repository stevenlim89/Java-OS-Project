#include "syscall.h"

int main (int argc, char **argv){

	char *str = "\nbye lucky";

        while(*str){
                write(1,str,1);
                str++;
        }
	exit(1); 
	return 0;
}
