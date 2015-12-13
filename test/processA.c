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
	int xVal = join(x, &st);
	
  printf("value of x after join: %d", xVal);
    
	char *str2 = "\nhi again lucky";
	while(*str2){
		write(1,str2,1);
		str2++;
	}
  //int y = join(x, &st);
  //printf("\ndisown %d", y); 

	return 0;

}

