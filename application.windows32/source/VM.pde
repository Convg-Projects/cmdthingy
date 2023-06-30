class VM {
  long[] memory;
  long[] regs;
  int stackStart;
  int stackSize;
  int regSize;
  int addressRegister = 0;
  int stackPointer = 0;
  int programCounter = 0;
  long instructionRegister = 0L;
  long operandRegister = 0L;

  int registerSelectorA;
  int registerSelectorB;
  int registerSelectorC;

  int maxPulseDelay;
  int pulseTimeLeft;

  boolean carry = false;

  VM(int MaxMem, int BusWidth, int Regs, int StackStart, int StackSize) {
    memory = new long[MaxMem];
    regs = new long[Regs];
    stackStart = StackStart;
    stackSize = StackSize;
    regSize = BusWidth;
  }

  void pulse(boolean displayStatus, int delay) {
    fetch();
    decode();
    execute();

    writeText("clock pulsed");
    if (displayStatus) {
      executeCommand(10, null); //logvm
    }
    display();

    long lastTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - lastTime) <= delay) {/*delay*/
    }
  }

  void fetch() {
    addressRegister = programCounter;
    ++programCounter;
    if (programCounter > memory.length - 1) {
      programCounter = 0;
    }
  }

  void decode() {
    instructionRegister = memory[addressRegister];
    /*++addressRegister;
     if (addressRegister > memory.length - 1) {
     addressRegister = 0;
     }*/
  }

  void execute() {
    //nop
    if (instructionRegister == 0) {
    }
    //ldi
    if (instructionRegister == 1) {
      incrementAddress();
      registerSelectorA = (int) memory[addressRegister];
      incrementAddress();
      if (registerSelectorA < regs.length) {
        regs[registerSelectorA] = memory[addressRegister];
      }
    }
    //add
    if (instructionRegister == 2) {
      incrementAddress();
      registerSelectorA = (int) memory[addressRegister];
      incrementAddress();
      registerSelectorB = (int) memory[addressRegister];
      incrementAddress();
      registerSelectorC = (int) memory[addressRegister];

      if (registerSelectorC >= regs.length) {
        return;
      }

      int result = 0;

      if (registerSelectorA < regs.length) {
        result += regs[registerSelectorA];
      }
      if (registerSelectorB < regs.length) {
        result += regs[registerSelectorB];
      }
      
      carry = false;
      while (result > 1L << regSize) {
        result -= 1L << regSize;
        carry = true;
      }

      regs[registerSelectorC] = result;
    }
    //jmp
    if (instructionRegister == 3) {
      incrementAddress();
      programCounter = (int) memory[addressRegister];
    }
    //sub
    if (instructionRegister == 4) {
      incrementAddress();
      registerSelectorA = (int) memory[addressRegister];
      incrementAddress();
      registerSelectorB = (int) memory[addressRegister];
      incrementAddress();
      registerSelectorC = (int) memory[addressRegister];

      if (registerSelectorC >= regs.length) {
        return;
      }

      int result = 0;

      if (registerSelectorA < regs.length) {
        result += regs[registerSelectorA];
      }
      if (registerSelectorB < regs.length) {
        result -= regs[registerSelectorB];
      }

      carry = false;
      while (result > 1L << regSize) {
        result -= 1L << regSize;
        carry = true;
      }

      regs[registerSelectorC] = result;
    }

    //lda
    if (instructionRegister == 5) {
      incrementAddress();
      registerSelectorA = (int) memory[addressRegister];
      incrementAddress();
      if (registerSelectorA < regs.length) {
        regs[registerSelectorA] = memory[(int) memory[addressRegister]];
      }
    }

    //sta
    if (instructionRegister == 6) {
      incrementAddress();
      registerSelectorA = (int) memory[(int) memory[addressRegister]];
      incrementAddress();
      if (registerSelectorA < regs.length) {
        memory[(int) memory[addressRegister]] = regs[registerSelectorA];
      }
    }

    //jsr
    if (instructionRegister == 7) {
      incrementAddress();
      memory[stackPointer + stackStart] = programCounter + 1;
      incrementSP();

      programCounter = (int) memory[addressRegister];
    }

    //rts
    if (instructionRegister == 8) {
      decrementSP();
      programCounter = (int) memory[stackPointer + stackStart];
    }

    //push
    if (instructionRegister == 9) {
      incrementAddress();
      registerSelectorA = (int) memory[addressRegister];
      if (registerSelectorA < regs.length) {
        memory[stackPointer + stackStart] = regs[registerSelectorA];
      }
      incrementSP();
    }

    //pop
    if (instructionRegister == 10) {
      incrementAddress();
      registerSelectorA = (int) memory[addressRegister];
      decrementSP();
      if (registerSelectorA < regs.length) {
        regs[registerSelectorA] = memory[stackPointer + stackStart] ;
      }
    }

    //bcs
    if (instructionRegister == 11) {
      incrementAddress();
      if(carry){
        programCounter = (int) memory[addressRegister];
      }
    }
  }

  void incrementAddress() {
    ++addressRegister;
    if (addressRegister > memory.length - 1) {
      addressRegister = 0;
    }
  }

  void incrementSP() {
    ++stackPointer;
    if (stackPointer > stackSize - 1) {
      stackPointer = 0;
    }
  }

  void decrementSP() {
    --stackPointer;
    if (stackPointer < 0) {
      stackPointer = stackSize - 1;
    }
  }

  void runClock(int pulses, int delay, boolean displayStatus) {
    pulsesLeft = pulses;
    pulseDelay = delay;
    pulseDisplayStatus = displayStatus;
  }

  void mov(int regA, int regB) {
    if (regA > regs.length - 1 || regB > regs.length - 1) {
      throwError("Invalid Input: There Are Only " + regs.length + " Registers Available");
      return;
    }

    if (regA < 0 || regB < 0) {
      throwError("Invalid Input: Negative Register");
      return;
    }

    regs[regB] = regs[regA];
    writeText("done");
  }

  void peek(int address) {
    if (address > memory.length - 1) {
      throwError("Invalid Address: Highest Address Is " + (memory.length - 1));
      return;
    }

    if (address < 0) {
      throwError("Invalid Address: Negative Address");
      return;
    }

    writeText(Long.toString(memory[address]));
  }

  void poke(int address, long data) {
    if (address > memory.length - 1) {
      throwError("Invalid Address: Highest Address Is " + (memory.length - 1));
      return;
    }

    if (address < 0) {
      throwError("Invalid Address: Negative Address");
      return;
    }

    if (data > 1L << regSize) {
      throwError("Invalid Data: Bus Width Is " + regSize + " Bits");
      return;
    }

    if (data < 0) {
      throwError("Invalid Data: Negative Data");
      return;
    }

    memory[address] = data;
    writeText("done");
  }

  void ldi(int reg, long data) {
    if (reg > regs.length - 1) {
      throwError("Invalid Input: There are only " + regs.length + " Registers Available");
      return;
    }

    if (reg < 0) {
      throwError("Invalid Input: Negative Register");
      return;
    }

    if (data > 1L << regSize) {
      throwError("Invalid Data: Bus Width Is " + regSize + " Bits");
      return;
    }

    if (data < 0) {
      throwError("Invalid Data: Negative Data");
      return;
    }

    regs[reg] = data;
    writeText("done");
  }

  void lda(int reg, int address) {
    if (reg > regs.length - 1) {
      throwError("Invalid Input: There are only " + regs.length + " Registers Available");
      return;
    }

    if (reg < 0) {
      throwError("Invalid Input: Negative Register");
      return;
    }

    if (address > memory.length - 1) {
      throwError("Invalid Address: Highest Address Is " + (memory.length - 1));
      return;
    }

    if (address < 0) {
      throwError("Invalid Address: Negative Address");
      return;
    }

    regs[reg] = memory[address];
    writeText("done");
  }
}
