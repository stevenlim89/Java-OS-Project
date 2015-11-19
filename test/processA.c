#include "syscall.h"



int st;

int main ( int argc, char ** argv)
{
	char *str = "\nhello lucky";  
	
	while(*str){
		write(1,str,1); 
		str++;
	}
	
 	int x = exec("processB.coff",0,0);
	join(x, &st);
		
	char *str2 = "\nhi again lucky";
	while(*str2){
		write(1,str2,1);
		str2++;
	}
	exit(1);
	return 0;

}

