# Project: consolehead
# Makefile created by Dev-C++ 4.9.9.2

CPP  = i586-mingw32msvc-g++
CC   = i586-mingw32msvc-gcc
WINDRES = i586-mingw32msvc-windres
RES  = 
OBJ  = ../../head/consolehead.o ../../head/head.o $(RES)
LINKOBJ  = ../../head/consolehead.o ../../head/head.o $(RES)
LIBS =  -n  -s 
INCS = 
CXXINCS =  
BIN  = consolehead.exe
CXXFLAGS = $(CXXINCS)   -fexpensive-optimizations -O3
CFLAGS = $(INCS)   -fexpensive-optimizations -O3
RM = rm -f

.PHONY: all all-before all-after clean clean-custom

all: all-before consolehead.exe all-after


clean: clean-custom
	${RM} $(OBJ) $(BIN)

$(BIN): $(OBJ)
#	$(CC) $(LINKOBJ) -o "consolehead.exe" $(LIBS)

../../head/consolehead.o: consolehead.c
	$(CC) -c consolehead.c -o ../../head/consolehead.o $(CFLAGS)

../../head/head.o: ../head.c
	$(CC) -c ../head.c -o ../../head/head.o $(CFLAGS)
