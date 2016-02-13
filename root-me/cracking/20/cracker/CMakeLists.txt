cmake_minimum_required(VERSION 3.3)
project(cracker)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES
    src/combination.cpp
    src/combination.h
    src/cracker.cpp
    src/data.cpp
    src/data.h
    src/Makefile)

add_executable(cracker ${SOURCE_FILES})