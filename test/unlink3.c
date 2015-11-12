#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int fd,size,i,j,ret;
int main(int argc, char** argv)
{
	ret=read(17,buf,20);
	if(ret!=-1)return -1;
	
	ret=write(17,buf,20);
	if(ret!=-1)return -1;
	
	ret=close(22);
	if(ret!=-1)return -1;
	
	ret=unlink("");
	if(ret!=-1)return -1;
	
	return 0;
}
