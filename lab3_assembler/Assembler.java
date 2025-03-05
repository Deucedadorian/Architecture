/* Assembler simulation program.
    Has 2 assembler passes:
          pass 1 builds symbol table from labels
          pass 2 uses symbol table to generate machine code
    CPU emulates machine code processing
    Labels and variables are resolved dynamically
    This program runs an infinite loop.
    DBreski  SP22
 */
import java.util.*;

public class Assembler {

    private static final Map<String, String> OPCODES = new HashMap<>();
    // added hashmap to store registers
    private static final Map<String, Integer> registerTable = new HashMap<>();
    private static final Map<String, Integer> symbolTable = new HashMap<>();
    private static final List<String> assemblyCode = new ArrayList<>();
    private static final List<String> machineCode = new ArrayList<>();
    private static final int[] memory = new int[256];
    private static int memoryAddress = 0;

    //Initialize registers in register table
    static {
        registerTable.put("A", 0);
        registerTable.put("B", 0);
        registerTable.put("C", 0);
        registerTable.put("D", 0);
    }

    private static int programCounter = 0;
    private static boolean isRunning = true;

    static {
        OPCODES.put("LOAD", "1");
        OPCODES.put("STORE", "2");
        OPCODES.put("ADD", "3");
        OPCODES.put("SUB", "4");
        OPCODES.put("JUMP", "5");
        OPCODES.put("HLT", "6");
        OPCODES.put("MUL", "7");
        OPCODES.put("DIV", "8");
        OPCODES.put("AND", "9");
        OPCODES.put("OR", "A");

    }

    private static void passOne(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" ");
            if (parts[0].endsWith(":")) {
                String label = parts[0].replace(":", "");
                symbolTable.put(label, memoryAddress);
            } else if (parts[0].equals("VAR")) {
                String varName = parts[1].replace(",", "");
                int value = Integer.parseInt(parts[2]);
                symbolTable.put(varName, memoryAddress);
                memory[memoryAddress] = value;
                memoryAddress++;
            } else {
                // check if operator is an immediate value
                if (parts.length > 2 && parts[2].charAt(0) == '#') {
                    memory[memoryAddress] = Integer.parseInt(parts[2].replace("#", ""));
                    parts[2] = String.valueOf(memoryAddress);
                    memoryAddress++;
                    line = String.format("%s %s %s #", parts[0], parts[1], parts[2]);
                }
                assemblyCode.add(line);
                memoryAddress++;
            }
        }
    }

    private static void passTwo() {
        for (String line : assemblyCode) {
            String[] parts = line.split(" ");
            String opcode = OPCODES.get(parts[0]);

            if (opcode == null) {
                System.out.println("Error: Unknown instruction - " + parts[0]);
                continue;
            }

            String operand = parts.length > 1 ? parts[1].replace(",", "") : "00";
            String operator = parts.length > 2 ? parts[2] : "";
            String operandCode, operatorCode;

            if (symbolTable.containsKey(operand)) {
                operandCode = String.format("%02x", symbolTable.get(operand));
            } else if (operand.matches("\\d+")) {
                operandCode = String.format("%02X", Integer.parseInt(operand));
            } else {
                // convert operand to hex
                operandCode = String.format("%02X", (int) operand.charAt(0));
            }
            
            if (symbolTable.containsKey(operator)) {
                operatorCode = String.format("%02X", symbolTable.get(operator));
            } else if (operator.matches("\\d+")) {
                operatorCode = String.format("%02X", Integer.parseInt(operator));
            } else {
                // convert operator to hex
                if (operator != "") { 
                        operatorCode = String.format("%02X", (int) operator.charAt(0));
                } else {
                    operatorCode = "00";
                }
            }
            
            String flag;
            if (parts.length > 3) {
                flag = "1";
            } else {
                flag = "0";
            }

            String machineInstr = opcode + operandCode + operatorCode + flag;
            System.out.println(machineInstr);
            System.out.println(opcode +" "+ operandCode +" "+ operatorCode);
            machineCode.add(machineInstr);
        }
    }

    private static String hexToBinary(String hex) {
        int decimal = Integer.parseInt(hex, 16);
        return String.format("%32s", Integer.toBinaryString(decimal)).replace(' ', '0');
    }

    private static void executeMachineCode() {
        programCounter = 0;
        isRunning = true;

        while (programCounter < machineCode.size() && isRunning) {
            String instruction = machineCode.get(programCounter);

            String opcode = instruction.substring(0, 1);
            String operand = instruction.substring(1, 3);
            String operator = instruction.substring(3, 5);
            String flag = instruction.substring(5, 6);

            int operandInt = Integer.parseInt(operand, 16);
            Character operandChar = (char)operandInt;
            String operandString = operandChar.toString();

            int operatorInt = Integer.parseInt(operator, 16);
            Character operatorChar = (char)operatorInt;
            String operatorString = operatorChar.toString();

            int op;

            switch (opcode) {
                case "1":

                    registerTable.replace(operandString, memory[operatorInt]);
                    System.out.println("LOAD " + operandString + ", " + 
                            operatorInt + " -> " + operandString + " = " + 
                            memory[operatorInt]);
                    break;

                case "2":
                    memory[operatorInt] = registerTable.get(operandString);
                    System.out.println("STORE " + operandString + ", " + 
                            operatorInt + " -> Memory[" + operatorInt + 
                            "] = " + registerTable.get(operandString));
                    break;

                case "3":

                    op = flag.equals("1") ? memory[operatorInt] : registerTable.get(operatorString);
                    registerTable.replace(operandString, registerTable.get(operandString) + op);
                    System.out.println("ADD " + operandString + ", " + 
                            op + " -> " + operandString + " = " + 
                            registerTable.get(operandString));
                    break;

                case "4":
                    op = flag.equals("1") ? memory[operatorInt] : registerTable.get(operatorString);
                    registerTable.replace(operandString, registerTable.get(operandString) - op);
                    System.out.println("SUB " + operandString +
                            ", "+ op + " -> " +
                            operandString + " = " + 
                        registerTable.get(operandString));
                    break;

                case "5":
                    System.out.println("JUMP " + operandString);
                    programCounter = memory[operandInt];
                    continue;

                case "6":
                    System.out.println("HLT");
                    isRunning = false;
                    break;

                case "7":
                    op = flag.equals("1") ? memory[operatorInt] : registerTable.get(operatorString);
                    registerTable.replace(operandString, registerTable.get(operandString) * op);
                    System.out.println("MUL " + operandString +
                            ", "+ op + " -> " +
                            operandString + " = " + 
                        registerTable.get(operandString));
                    break;

                case "8":
                    op = flag.equals("1") ? memory[operatorInt] : registerTable.get(operatorString);
                    registerTable.replace(operandString, registerTable.get(operandString) / op);
                    System.out.println("DIV " + operandString +
                            ", "+ op + " -> " +
                            operandString + " = " + 
                        registerTable.get(operandString));
                    break;

                case "9":
                    // AND operation
                    op = flag.equals("1") ? memory[operatorInt] : registerTable.get(operatorString);
                    registerTable.replace(operandString, registerTable.get(operandString) & op);
                    System.out.println("AND " + operandString +
                            ", "+ op + " -> " +
                            operandString + " = " + 
                        registerTable.get(operandString));
                    break;

                case "A":
                    // OR operation
                    op = flag.equals("1") ? memory[operatorInt] : registerTable.get(operatorString);
                    registerTable.replace(operandString, registerTable.get(operandString) | op);
                    System.out.println("OR " + operandString +
                            ", "+ op + " -> " +
                            operandString + " = " + 
                        registerTable.get(operandString));
                    break;

                default:
                    System.out.println("Unknown opcode: " + opcode);
            }
            programCounter++;
        }
    }

    public static void main(String[] args) {
        String[] assemblyProgram = {
                "VAR X, 5",
                "LOAD A, X",
                "AND A, #7",
                "LOAD B, #1",
                "OR B, #1",
                //"JUMP LOOP",  // 🔄 Should now correctly loop!
                 
        };

        passOne(assemblyProgram);
        passTwo();

        System.out.println("Symbol Table:");
        symbolTable.forEach((key, value) -> System.out.println(key + " -> " + memory[value]));

        System.out.println("\nMachine Code (Hex & Binary):");
        for (String code : machineCode) {
            System.out.println(code + "  -->  " + hexToBinary(code));
        }

        System.out.println("\nExecuting Machine Code:");
        executeMachineCode();
    }
}
