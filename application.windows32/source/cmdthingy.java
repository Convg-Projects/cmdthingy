import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class cmdthingy extends PApplet {

char[][] characters = new char[40][80];
byte rowPointer = 0;
byte columnPointer = 0;

char[] acceptedChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
  '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
  '!', '"', '£', '$', '%', '^', '&', '*', '(', ')', ' ', '<', '>', ',', '.', '/', '?', '#', '~', '@', ';', ':', '[', ']', '{', '}', '-', /*'=',*/ '+', '_', TAB, '|', '\\'};

String[] commandList =   {"echo", "help", "add", "mult", "sub", "divr", "createvm", "destroyvm", "clear", "mov", "logvm", "peek", "poke", "ldi", "lda", "pulse", "run", "instlist", "welcome"};
int[] commandParams =    {     1,      0,     2,      2,     2,      2,          5,           0,       0,     2,       0,      1,      2,     2,     2,       1,     3,          0,         0};

int historyPointer = 0;
String[] lastCommands = new String[256];

VM mainVM;
boolean VMEnabled = false;

float cursorTime = 1f;
boolean cursorEnabled = true;

int pulsesLeft = 0;
int pulseDelay = 0;
boolean pulseDisplayStatus = false;

public void setup() {
  
  frameRate(1000);

  for (int i = 0; i < lastCommands.length; ++i) {
    lastCommands[i] = "";
  }

  PFont mainFont;
  mainFont = createFont("kongtext.ttf", 128);
  background(0);
  textFont(mainFont);
  textSize(16);
  
  writeText("Type 'help' for a list of commands");
  writeText("Type 'instlist' for a list of available CPU instructions");
  writeText("Type 'welcome' to see this message again");
  writeText("");
  writeText("");
  writeText("");
  writeText("");
}



public void draw() {
  display();

  if (cursorEnabled) {
    rect(columnPointer * 16 - 2, rowPointer * 16 + 18, 3, -18);
  }
  
  cursorTime -= 1 / frameRate;
  if(cursorTime <= 0){
    cursorTime = 1;
    cursorEnabled = !cursorEnabled;
  }
  
  if(pulsesLeft > 0){
    mainVM.pulse(pulseDisplayStatus, pulseDelay);
    --pulsesLeft;
  }
}



public void display() {
  background(0);

  for (int i = 0; i < characters.length; ++i) {
    for (int j = 0; j < characters[i].length; ++j) {
      if (characters[i][j] != 0) {
        text(characters[i][j], j * 16, i * 16 + 16);
      }
    }
  }
}



public void run() {
  String command = "";
  int commandValue = -1;
  String[] parameters = new String[255];

  //Get command
  while (characters[rowPointer - 1][columnPointer] != ' ' && characters[rowPointer - 1][columnPointer] != 0) {
    command += characters[rowPointer - 1][columnPointer];
    ++columnPointer;
  }

  for (int i = 0; i < commandList.length; ++i) {
    if (command.equals(commandList[i])) {
      commandValue = i;
      break;
    }
  }

  if (commandValue == -1) {
    throwError("Unknown command");
    columnPointer = 0;
    return;
  }

  //Get parameters

  ++columnPointer;
  for (int i = 0; i < commandParams[commandValue]; ++i) {
    parameters[i] = "";
    while (characters[rowPointer - 1][columnPointer] != ' ' && characters[rowPointer - 1][columnPointer] != 0) {
      parameters[i] += characters[rowPointer - 1][columnPointer];
      ++columnPointer;
    }

    if (characters[rowPointer - 1][columnPointer] == 0 && i != commandParams[commandValue] - 1) {
      if (commandParams[commandValue] == 1) {
        throwError("Invalid Input: Expected 1 Parameter");
      } else {
        throwError("Invalid Input: Expected " + commandParams[commandValue] + " Parameters");
      }
      columnPointer = 0;
      return;
    }
    ++columnPointer;
  }

  executeCommand(commandValue, parameters);

  columnPointer = 0;
}



public void throwError(String error) {
  columnPointer = 0;
  for (int i = 0; i < error.length(); ++i) {
    characters[rowPointer][columnPointer] = error.charAt(i);
    ++columnPointer;
  }
  newLine();
}



public void writeText(String text) {
  columnPointer = 0;
  for (int i = 0; i < text.length(); ++i) {
    characters[rowPointer][columnPointer] = text.charAt(i);
    ++columnPointer;
  }
  newLine();
}



public void newLine() {
  if (rowPointer == characters.length - 1) {
    for (int i = 0; i < characters.length - 1; ++i) {
      characters[i] = characters[i + 1];
    }
    characters[characters.length - 1] = new char[characters[0].length];
    columnPointer = 0;
    return;
  }

  ++rowPointer;
  columnPointer = 0;
}

//================================================================================================================================================================================================
//COMMANDS
//================================================================================================================================================================================================

public void executeCommand(int commandPointer, String[] parameters) {
  //echo | string text;
  if (commandPointer == 0) {
    writeText(parameters[0]);
    return;
  }

  //help
  if (commandPointer == 1) {
    writeText("Command List:");
    for (int i = 0; i < commandList.length; ++i) {
      if(commandParams[i] == 1){
        writeText(commandList[i] + "..........(Expects 1 Parameter)");
      } else {
        writeText(commandList[i] + "..........(Expects " + commandParams[i] + " Parameters)");
      }
    }
    return;
  }

  //add | int a; int b
  if (commandPointer == 2) {
    int a;
    int b;

    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      b = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > add [int a] [int b]");
      return;
    }

    writeText(Integer.toString(a + b));
    return;
  }

  //mult | int a; int b
  if (commandPointer == 3) {
    int a;
    int b;

    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      b = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > mult [int a] [int b]");
      return;
    }

    writeText(Integer.toString(a * b));
    return;
  }

  //sub | int a; int b
  if (commandPointer == 4) {
    int a;
    int b;

    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      b = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > sub [int a] [int b]");
      return;
    }

    writeText(Integer.toString(a - b));
    return;
  }

  //divr | int a; int b
  if (commandPointer == 5) {
    int a;
    int b;

    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      b = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > div [int a] [int b]");
      return;
    }

    writeText(Integer.toString(a / b));
    return;
  }

  //createvm | string text;
  if (commandPointer == 6) {
    int maxMem;
    int busWidth;
    int regs;
    int stackStart;
    int stackSize;

    try {
      Integer.parseInt(parameters[0]);
      maxMem = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      busWidth = Integer.parseInt(parameters[1]);
      Integer.parseInt(parameters[2]);
      regs = Integer.parseInt(parameters[2]);
      Integer.parseInt(parameters[3]);
      stackStart = Integer.parseInt(parameters[3]);
      Integer.parseInt(parameters[4]);
      stackSize = Integer.parseInt(parameters[4]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > createvm [int maxMem] [int busWidth] [int regs]");
      throwError("[int stackStart] [int stackSize]");
      return;
    }
    if (VMEnabled) {
      throwError("There Is Already An Active Virtual Machine. Destroy It First");
      return;
    }
    
    if (maxMem < 1) {
      throwError("Invalid Input: VM Cannot Have No Memory");
      return;
    }
    
    if (stackSize < 1) {
      throwError("Invalid Input: VM Cannot Have No Stack");
      return;
    }
    
    if (stackStart < 0) {
      throwError("Invalid Input: Negative Stack Address");
      return;
    }
    
    if (regs < 2) {
      throwError("Invalid Input: VM Must Have At Least 2 Registers");
      return;
    }

    if (busWidth != 8 && busWidth != 16 && busWidth != 32) {
      throwError("Invalid Input: Available Bus Widths Are 8, 16, And 32");
      return;
    }

    if (maxMem > 1L << busWidth) {
      throwError("Invalid Input: Memory Must Be Addressable Within Bus Width");
      return;
    }

    if (maxMem > 1048576) {
      throwError("Invalid Input: Maximum Allowed Memory Allocated is 1MiB");
      return;
    }

    if (stackSize > maxMem) {
      throwError("Invalid Input: Stack Size Cannot Exceed Allocated Memory");
      return;
    }

    if (stackStart + stackSize > maxMem) {
      throwError("Invalid Input: Allocated Stack Space Cannot Exceed Address Space");
      return;
    }

    if (regs > 32) {
      throwError("Invalid Input: A Maximum Of 32 Registers Is Allowed");
      return;
    }

    mainVM = new VM(maxMem, busWidth, regs, stackStart, stackSize);
    VMEnabled = true;
    writeText("done");

    return;
  }

  //destroyvm
  if (commandPointer == 7) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }

    mainVM = null;
    VMEnabled = false;
    writeText("done");
    return;
  }
  
  //clear
  if (commandPointer == 8) {
    characters = new char[40][80];
    columnPointer = 0;
    rowPointer = 0;
    
    return;
  }
  
  //mov
  if (commandPointer == 9) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    int a;
    int b;

    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      b = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > mov [int a] [int b]");
      return;
    }

    mainVM.mov(a, b);
    return;
  }
  
  //logvm
  if (commandPointer == 10) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    writeText("");
    for(int i = 0; i < mainVM.regs.length - 1; i += 2){
      writeText((i < 10 ? "Register  " : "Register ") + i + ":" + mainVM.regs[i] + "    " + 
                (i < 10 ? "Register  " : "Register ") + (i + 1) + ":" + mainVM.regs[i + 1]);
    }
    writeText("Current Address:" + mainVM.addressRegister + "   Value At Address: " + mainVM.memory[mainVM.addressRegister]);
    writeText("Program Counter:" + mainVM.programCounter  + "   Value At Address: " + mainVM.memory[mainVM.programCounter]);
    writeText("Stack Pointer  :" + mainVM.stackPointer    + "   Value At Address: " + mainVM.memory[mainVM.stackPointer]);
    writeText("Instruction    :" + mainVM.instructionRegister + "   Operand         : " + mainVM.operandRegister);
    
    return;
  }
  
  //peek | int address;
  if (commandPointer == 11) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    int a;

    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > peek [int address]");
      return;
    }
    
    mainVM.peek(a);
    
    return;
  }
  
  //poke | int address; int data
  if (commandPointer == 12) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    int a;
    long b;
    
    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Long.parseLong(parameters[1]);
      b = Long.parseLong(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > poke [int address] [uint data]");
      return;
    }
    
    mainVM.poke(a, b);
    
    return;
  }
  
  //ldi | int reg; int data
  if (commandPointer == 13) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    int a;
    long b;
    
    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Long.parseLong(parameters[1]);
      b = Long.parseLong(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > ldi [int reg] [uint data]");
      return;
    }
    
    mainVM.ldi(a, b);
    
    return;
  }
  
  //lda | int address; int data
  if (commandPointer == 14) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    int a;
    int b;
    
    try {
      Integer.parseInt(parameters[0]);
      a = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      b = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > poke [int reg] [uint address]");
      return;
    }
    
    mainVM.lda(a, b);
    
    return;
  }
  
  //pulse | boolean displayStatus
  if (commandPointer == 15) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    if(parameters[0].equals("true")){
      mainVM.pulse(true, 0);
      return;
    }
    
    if(parameters[0].equals("false")){
      mainVM.pulse(false, 0);
      return;
    }
    
    throwError("Invalid Input: Expected > pulse [bool displayStatus]");
    return;
  }
  
  //run | boolean displayStatus
  if (commandPointer == 16) {
    if (!VMEnabled) {
      throwError("There Is No Virtual Machine Available");
      return;
    }
    
    int pulses;
    int delay;
    boolean displayStatus = false;
    
    try {
      Integer.parseInt(parameters[0]);
      pulses = Integer.parseInt(parameters[0]);
      Integer.parseInt(parameters[1]);
      delay = Integer.parseInt(parameters[1]);
    }
    catch( Exception e ) {
      throwError("Invalid Input: Expected > run [int pulses] [int delay] [bool displayStatus]");
      return;
    }
    
    if(!parameters[2].equals("true") && !parameters[2].equals("false")){
      throwError("Invalid Input: Expected > run [int pulses] [int delay] [bool displayStatus]");
    }
    
    if(parameters[2].equals("true")){
      displayStatus = true;
    }
    
    if(parameters[2].equals("false")){
      displayStatus = false;
    }
    
    writeText("running...");
    display();
    mainVM.runClock(pulses, delay, displayStatus);
    return;
  }
  
  //instlist
  if (commandPointer == 17) { 
    writeText("Emulator Instruction Set:");
    writeText("0: NOP");
    writeText("1: LDI reg < [imm]");
    writeText("2: ADD reg + reg > reg");
    writeText("3: JMP [imm]");
    writeText("4: SUB reg - reg > reg");
    writeText("5: LDA reg < (imm)");
    writeText("6: STA reg > (imm)");
    writeText("7: JSR [imm]");
    writeText("8: RTS");
    writeText("9: PSH reg > stack");
    writeText("10: POP reg < stack");
    writeText("11: BCS [imm]");
    return;
  }
  
  //welcome
  if (commandPointer == 18) { 
    characters = new char[40][80];
    columnPointer = 0;
    rowPointer = 0;
    
    writeText("Type 'help' for a list of commands");
    writeText("Type 'instlist' for a list of available CPU instructions");
    writeText("Type 'welcome' to see this message again");
    writeText("");
    writeText("");
    writeText("");
    writeText("");
    return;
  }
}

//================================================================================================================================================================================================
//KEYBINDS
//================================================================================================================================================================================================

public void keyPressed() {
  if(pulsesLeft > 0){
    return;
  }
  
  if (key == BACKSPACE) {
    cursorTime = 1f;
    cursorEnabled = true;
    
    historyPointer = 0;
    if (columnPointer != 0) {
      --columnPointer;
      for (int i = columnPointer; i < characters[0].length - 1; ++i) {
        characters[rowPointer][i] = characters[rowPointer][i + 1];
      }
    }
    return;
  }

  if (key == ENTER) {
    cursorTime = 1f;
    cursorEnabled = true;
    
    historyPointer = 0;

    for (int i = lastCommands.length - 1; i > 1; --i) {
      lastCommands[i] = lastCommands[i - 1];
    }
    lastCommands[1] = String.valueOf(characters[rowPointer]);
    lastCommands[0] = "";

    newLine();
    run();
    return;
  }

  if (keyCode == LEFT && columnPointer != 0) {
    cursorTime = 1f;
    cursorEnabled = true;
    
    --columnPointer;
    return;
  }

  if (keyCode == RIGHT && columnPointer != characters[0].length - 1 && characters[rowPointer][columnPointer] != 0) {
    cursorTime = 1f;
    cursorEnabled = true;
    
    ++columnPointer;
    return;
  }

  if (keyCode == UP) {
    cursorTime = 1f;
    cursorEnabled = true;
    
    if (historyPointer == lastCommands.length - 1) {
      return;
    }
    ++historyPointer;

    columnPointer = 0;
    byte tempPointer = -1;
    for (int i = 0; i < lastCommands[historyPointer].length(); ++i) {
      characters[rowPointer][columnPointer] = lastCommands[historyPointer].charAt(i);
      
      if(characters[rowPointer][columnPointer] == 0 && tempPointer == -1){
        tempPointer = columnPointer;
      }
      
      ++columnPointer;
    }
    columnPointer = tempPointer;

    return;
  }

  if (keyCode == DOWN) {
    cursorTime = 1f;
    cursorEnabled = true;
    
    if (historyPointer == 0) {
      return;
    }
    --historyPointer;

    columnPointer = 0;
    byte tempPointer = -1;
    for (int i = 0; i < lastCommands[historyPointer].length(); ++i) {
      characters[rowPointer][columnPointer] = lastCommands[historyPointer].charAt(i);
      
      if(characters[rowPointer][columnPointer] == 0 && tempPointer == -1){
        tempPointer = columnPointer;
      }
      
      ++columnPointer;
    }
    columnPointer = tempPointer;
    
    if(historyPointer == 0){
      columnPointer = 0;
      for (int i = 0; i < characters[0].length; ++i) {
        characters[rowPointer][columnPointer] = 0;
        ++columnPointer;
      }
      columnPointer = 0;
    }

    return;
  }

  if (columnPointer == characters[0].length - 1) {
    return;
  }

  for (int i = 0; i < acceptedChars.length; ++i) {
    if (key == acceptedChars[i]) {
      cursorTime = 1f;
      cursorEnabled = true;
      
      historyPointer = 0;
      
      if (characters[rowPointer][columnPointer] != 0) {
        for (int j = characters[0].length - 3; j > columnPointer - 1; --j) {
          characters[rowPointer][j + 1] = characters[rowPointer][j];
        }
      }
      characters[rowPointer][columnPointer] = key;
      ++columnPointer;
    }
  }
}
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

  public void pulse(boolean displayStatus, int delay) {
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

  public void fetch() {
    addressRegister = programCounter;
    ++programCounter;
    if (programCounter > memory.length - 1) {
      programCounter = 0;
    }
  }

  public void decode() {
    instructionRegister = memory[addressRegister];
    /*++addressRegister;
     if (addressRegister > memory.length - 1) {
     addressRegister = 0;
     }*/
  }

  public void execute() {
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

  public void incrementAddress() {
    ++addressRegister;
    if (addressRegister > memory.length - 1) {
      addressRegister = 0;
    }
  }

  public void incrementSP() {
    ++stackPointer;
    if (stackPointer > stackSize - 1) {
      stackPointer = 0;
    }
  }

  public void decrementSP() {
    --stackPointer;
    if (stackPointer < 0) {
      stackPointer = stackSize - 1;
    }
  }

  public void runClock(int pulses, int delay, boolean displayStatus) {
    pulsesLeft = pulses;
    pulseDelay = delay;
    pulseDisplayStatus = displayStatus;
  }

  public void mov(int regA, int regB) {
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

  public void peek(int address) {
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

  public void poke(int address, long data) {
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

  public void ldi(int reg, long data) {
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

  public void lda(int reg, int address) {
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
  public void settings() {  size(1280, 640); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "cmdthingy" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
