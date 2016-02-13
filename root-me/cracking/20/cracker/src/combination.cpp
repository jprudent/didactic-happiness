#include <iostream>
#include <cmath>
#include <stdlib.h>
#include "combination.h"

using namespace std;

void setCombination(unsigned char * a, unsigned char v) {
    if(a[0] == 0) {  // 0 is the last element
        return;
    }

    a[0] = v;
    setCombination(a+1,v);
}

unsigned char incCombination(unsigned char * a) {

    if(a[0] == 0) {  // 0 is the last element
        return 0;
    }

    unsigned char next = incCombination(a+1);
    if(next == 0){
        if(a[0] >= MAX_COMB){
            return 0;
        } else {
            setCombination(a+1, MIN_COMB);
            return a[0] += 1;
        }
    }
    return next;
}

int lenCombination(const unsigned char * a) {
     if (a[0] == 0){
        return 0;
     } else {
        return lenCombination(a+1) + 1;
     }
}

void printCombination(const unsigned char * a) {
    if (a[0] == 0){
        cout << endl;
    } else {
        cout << a[0] << "|";
        printCombination(a+1);
    }
}

unsigned char * makeCombination(int key_len){
     unsigned char * combination = (unsigned char *) malloc(key_len * sizeof(unsigned char));
     for(int i=0; i<key_len; i++){
        combination[i] = MIN_COMB;
     }
     combination[key_len] = 0;
     return combination;
}

int test_combinaion(void) {

    const int key_len = 5;
    unsigned char * combination = makeCombination(key_len);
    double nbCombinations = pow(RANGE_COMB, lenCombination(combination));
    cout << "expected " << nbCombinations << " combinations for a key of " << lenCombination(combination) << endl;
    for(double i=0; i < nbCombinations; i++){
        incCombination(combination);
        if(fmod(i,1000000000) == 0){
            cout << "n : " << i << endl;
            printCombination(combination);
        }
    }
    return 0;
}