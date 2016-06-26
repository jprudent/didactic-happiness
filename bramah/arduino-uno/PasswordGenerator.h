#ifndef PASSWORD_GENERATOR_H
#define PASSWORD_GENERATOR_H

#include <stdio.h>

extern const size_t HLEN;

class PasswordGenerator {
    public:
        PasswordGenerator();
        ~PasswordGenerator();
        void generate_password(char * output, const char * master_password, const char * memo); 
    private:
        void encode_to_ascii(char * output, char * input); 
        void hash(char * output, const char * s1, const char * s2); 
};

#endif
