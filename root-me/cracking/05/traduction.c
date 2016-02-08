#include <stdio.h>

int main(int argc, char ** argv) {
    int ecx = argc;
    int ebp0x9c = argv; // ??

    int ebp0xc = gs0x14;
    // sub esp, 0xb0

    if(ebp0x9c == 2) {
      exit(0);
    }
    int ebp0x94 = malloc(30);
    memcpy(0x1f, 0x8048910, ebp0x94);
    /*
     * "_0cGj35m9V5T3Ã8CJ0Ã9H95h3xdh"
     * 0x8048910:      0x5f    0x30    0x63    0x47    0x6a    0x33    0x35
     * 0x6d
     * 0x8048918:      0x39    0x56    0x35    0x54    0x33    0xc3    0x87
     * 0x38
     * 0x8048920:      0x43    0x4a    0x30    0xc3    0x80    0x39    0x48
     * 0x39
     * 0x8048928:      0x35    0x68    0x33    0x78    0x64    0x68    0x00
     */
    int ebp0xa4, edi = 0xffffd67a; //4294956666 
    int ebp0xa8, eax = 0;
    int ebp0xac, ecx = 0x19; //25
    copyNTimes(ecx, eax, es:[edi]);
    memcpy(13, 0x804892f, ebp-0x8e<)//"_Celebration" 
        
        
        //code qui modifie ebp-0x94
        //0x804b008:      "_0cGjc5m9V5T3Ã8CJ0Ã9"

    // mov    DWORD PTR ds:0x804a038,0x80486c4 

    strcpy(ebp0x2a, argv[1])
    
    //code qui modifie ebp-0x94
    // 0x804b008:      "_0cGjc5m_.5T3Ã8CJ0Ã9"
    WPA(ebp0x2a, ebp0x94)
    
}

int WPA(char * s1, char * s2) {
    int l1 =
        int l2 = 
    // code qui modifie 0x804B008 
    // 0x804b008:      "_0cGjc5m_.5\r\nÃ8CJ0Ã9"
    if(strcmp(0x804B008, "foo")){

    }

}

int f() {

}
