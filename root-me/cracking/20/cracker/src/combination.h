unsigned char * makeCombination(int key_len);
void printCombination(const unsigned char * a);
int lenCombination(const unsigned char * a);
unsigned char incCombination(unsigned char * a);

#define MAX_COMB 126
//const unsigned char MAX_COMB = 57;
#define MIN_COMB 32
//const unsigned char MIN_COMB = 48;
#define RANGE_COMB (MAX_COMB - MIN_COMB + 1)