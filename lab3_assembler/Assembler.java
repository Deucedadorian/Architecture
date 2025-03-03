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
    private static final Map<String, Integer> registerTable = new HashMap<>();
    private static final Map<String, Integer> symbolTable = new HashMap<>();
    private static final List<String> assemblyCode = new ArrayList<>();
    private static final List<String> machineCode = new ArrayList<>();
    private static final int[] memory = new int[256];
    private static int memoryAddress = 0;

    // initialize registers
    static {
        registerTable.put("A", 0);
        registerTable.put("B", 0);
        registerTable.put("C", 0);
        registerTable.put("D", 0);
    }

    private static int programCounter = 0;
    private static boolean isRunning = true;

    static {
        OPCODES.put("LOAD", "01");
        OPCODES.put("STORE", "02");
        OPCODES.put("ADD", "03");
        OPCODES.put("SUB", "04");
        OPCODES.put("JUMP", "05");
        OPCODES.put("HLT", "06");
        OPCODES.put("MUL", "07");
        OPCODES.put("DIV", "08");
        OPCODES.put("AND", "09");
        OPCODES.put("OR", "10");

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

            String machineInstr = opcode + operandCode + operatorCode;
            System.out.println(opcode +" "+ operandCode +" "+ operatorCode);
            machineCode.add(machineInstr);
        }
    }

    private static String hexToBinary(String hex) {
        int decimal = Integer.parseInt(hex, 16);
        return String.format("%16s", Integer.toBinaryString(decimal)).replace(' ', '0');
    }

    private static void executeMachineCode() {
        programCounter = 0;
        isRunning = true;

        while (programCounter < machineCode.size() && isRunning) {
            String instruction = machineCode.get(programCounter);
            String opcode = instruction.substring(0, 2);
            String operand = instruction.substring(2, 4);
            String operator = instruction.substring(4, 6);
            int operandValue = Integer.parseInt(operand, 16);
            int operatorValue = Integer.parseInt(operator, 16);

            switch (opcode) {
                case "01":
                    registerTable.replace(""+(char)operandValue, memory[operatorValue]);
                    System.out.println("LOAD " + (char)operandValue + ", " + operatorValue + " -> " + (char)operandValue + " = " + memory[operatorValue]);
                    break;

                case "02":
                    memory[operatorValue] = registerTable.get(""+(char)operandValue);
                    System.out.println("STORE " + (char)operandValue + ", " + 
                            operatorValue + " -> Memory[" + operatorValue + 
                            "] = " + registerTable.get(""+(char)operandValue));
                    break;

                case "03":
                    registerTable.replace(""+(char)operandValue, 
                            registerTable.get(""+(char)operandValue) + 
                            registerTable.get(""+(char)operatorValue));
                    System.out.println("ADD " + (char)operandValue +
                            ", "+ (char)operatorValue + " -> A = " + 
                        registerTable.get(""+(char)operandValue));
                    break;

                case "04":
                    registerTable.replace(""+(char)operandValue, 
                            registerTable.get(operandValue) * 
                            registerTable.get(operatorValue));
                    System.out.println("SUB A, B -> A = " + 
                            registerTable.get((char)operandValue));
                    break;

                case "05":
                    System.out.println((char)operandValue + (char)operatorValue);
                    System.out.println("JUMP " + (char)operandValue);
                    programCounter = (char)operandValue - 1;
                    continue;

                case "06":
                    System.out.println("HLT");
                    isRunning = false;
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
                "LOAD B, X",
                "ADD A, B",
                "STORE A, 10",
                "LOOP: SUB A, B",
                // "JUMP LOOP",  // 🔄 Should now correctly loop!
                 
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
