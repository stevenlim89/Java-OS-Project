#include "syscall.h"

int addr;
int main(int argc, char *argv[]){
  char *str = "I wanna test my unexpected exception!";

  while(*str) {
    write(1, str, 1);
    str++;
  }

  int x = exec("dividebyzero.coff", 0, 0);
  int xVal = join(x, &addr);
  printf("value of x after first join: %d", xVal);
  char *str2 = "back!";

  while(*str2) {
    write (1, str2, 1);
    str2++;
  }
  xVal = join(x, &addr);
  printf("value of x after second join: %d", xVal);
  return 0;
}
