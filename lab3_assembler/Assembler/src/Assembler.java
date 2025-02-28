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
    private static final Map<String, Integer> symbolTable = new HashMap<>();
    private static final List<String> assemblyCode = new ArrayList<>();
    private static final List<String> machineCode = new ArrayList<>();
    private static final int[] memory = new int[256];
    private static int memoryAddress = 0;

    private static int regA = 0;
    private static int regB = 0;
    private static int programCounter = 0;
    private static boolean isRunning = true;

    static {
        OPCODES.put("LOAD", "1");
        OPCODES.put("STORE", "2");
        OPCODES.put("ADD", "3");
        OPCODES.put("SUB", "4");
        OPCODES.put("JUMP", "5");

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
            String operandCode;

            if (symbolTable.containsKey(operand)) {
                operandCode = String.format("%02X", symbolTable.get(operand));
            } else if (operand.matches("\\d+")) {
                operandCode = String.format("%02X", Integer.parseInt(operand));
            } else {
                operandCode = "00";
            }

            String machineInstr = opcode + operandCode;
            machineCode.add(machineInstr);
        }
    }

    private static String hexToBinary(String hex) {
        int decimal = Integer.parseInt(hex, 16);
        return String.format("%12s", Integer.toBinaryString(decimal)).replace(' ', '0');
    }

    private static void executeMachineCode() {
        programCounter = 0;
        isRunning = true;

        while (programCounter < machineCode.size() && isRunning) {
            String instruction = machineCode.get(programCounter);
            String opcode = instruction.substring(0, 1);
            String operand = instruction.substring(1, 3);
            int operandValue = Integer.parseInt(operand, 16);

            switch (opcode) {
                case "1":
                    regA = memory[operandValue];
                    System.out.println("LOAD A, " + operandValue + " -> A = " + regA);
                    break;

                case "2":
                    memory[operandValue] = regA;
                    System.out.println("STORE A, " + operandValue + " -> Memory[" + operandValue + "] = " + regA);
                    break;

                case "3":
                    regA += regB;
                    System.out.println("ADD A, B -> A = " + regA);
                    break;

                case "4":
                    regA -= regB;
                    System.out.println("SUB A, B -> A = " + regA);
                    break;

                case "5":
                    System.out.println("JUMP " + operandValue);
                    programCounter = operandValue - 1;
                    continue;

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
                "ADD A, B",
                "STORE A, 10",
                "LOOP: SUB A, B",
                "JUMP LOOP",  // Loops forever

        };

        passOne(assemblyProgram);
        passTwo();

        System.out.println("Symbol Table:");
        symbolTable.forEach((key, value) -> System.out.println(key + " -> " + value));

        System.out.println("\nMachine Code (Hex & Binary):");
        for (String code : machineCode) {
            System.out.println(code + "  -->  " + hexToBinary(code));
        }

        System.out.println("\nExecuting Machine Code:");
        executeMachineCode();
    }
}
