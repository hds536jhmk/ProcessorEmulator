package io.github.hds.pemu.compiler;

import io.github.hds.pemu.instructions.Instruction;
import io.github.hds.pemu.instructions.InstructionSet;
import io.github.hds.pemu.utils.StringUtils;
import io.github.hds.pemu.utils.Token;
import io.github.hds.pemu.utils.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {

    protected static class Tokens {

        public static final Token COMMENT   = new Token(";");
        public static final Token CONSTANT  = new Token("@");
        public static final Token LABEL     = new Token(":");
        public static final Token COMPILER  = new Token("#");
        public static final Token STRING    = new Token("\"'");
        public static final Token CHARACTER = new Token("'");
        public static final Token ESCAPE_CH = new Token("\\", true);
        public static final Token ARR_START = new Token("{");
        public static final Token ARR_END   = new Token("}");
        public static final Token OFF_START = new Token("[", true);
        public static final Token OFF_END   = new Token("]", true);
        public static final Token SPACE     = new Token("\\s");

        public static final Token[] ALL_TOKENS = new Token[] {
                COMMENT, CONSTANT, LABEL, COMPILER, STRING, CHARACTER, ESCAPE_CH, ARR_START, ARR_END, OFF_START, OFF_END, SPACE
        };

    }

    protected static class LabelData {
        public static int NULL_PTR = -1;

        public int pointer = NULL_PTR;
        public ArrayList<Integer> occurrences = new ArrayList<>();
        public ArrayList<Integer> offsets = new ArrayList<>();
        public LabelData() { }

        public LabelData(int pointer) {
            this.pointer = pointer;
        }

        public void addOccurrence(int at) {
            addOccurrence(at, 0);
        }

        public void addOccurrence(int at, int offset) {
            occurrences.add(at);
            offsets.add(offset);
        }
    }

    protected static class CompilerData {
        @NotNull ArrayList<Integer> program;
        @NotNull HashMap<String, LabelData> labels;
        @NotNull HashMap<String, Integer> constants;
        @NotNull Tokenizer tokenizer;

        protected CompilerData() {
            program = new ArrayList<>();
            labels = new HashMap<>();
            constants = Constants.getDefaultConstants();
            tokenizer = new Tokenizer();
        }
    }

    public static class SyntaxError extends RuntimeException {
        protected SyntaxError(@NotNull String expected, @Nullable String got, @NotNull Tokenizer tokenizer) {
            this(expected, got, tokenizer.getConsumedLines() + 1, tokenizer.getConsumedLineCharacters());
        }

        protected SyntaxError(@NotNull String expected, @Nullable String got, int currentLine, int currentChar) {
            super(String.format("Syntax Error (%d:%d): Expected %s, got '%s'.", currentLine, currentChar, expected, got));
        }
    }

    public static class ReferenceError extends RuntimeException {
        protected ReferenceError(@NotNull String type, @NotNull String name, @NotNull Tokenizer tokenizer) {
            this(type, name, tokenizer.getConsumedLines() + 1, tokenizer.getConsumedLineCharacters());
        }

        protected ReferenceError(@NotNull String type, @NotNull String name, int currentLine, int currentChar) {
            super(String.format("Reference Error (%d:%d): %s '%s' was not declared.", currentLine, currentChar, type, name));
        }
    }

    public static class TypeError extends RuntimeException {
        protected TypeError(@NotNull String message, @NotNull Tokenizer tokenizer) {
            this(message, tokenizer.getConsumedLines() + 1, tokenizer.getConsumedLineCharacters());
        }

        protected TypeError(@NotNull String message, int currentLine, int currentChar) {
            super(String.format("Type Error (%d:%d): %s.", currentLine, currentChar, message));
        }
    }

    protected enum PARSE_STATUS {
        SUCCESS, FAIL, SUCCESS_PROGRAM_NOT_CHANGED
    }

    protected static class ParseResult <T> {
        public final PARSE_STATUS STATUS;
        public final String NAME;
        public final T VALUE;

        protected ParseResult(PARSE_STATUS status) {
            this(status, null, null);
        }

        protected ParseResult(PARSE_STATUS status, String name, T value) {
            STATUS = status;
            NAME = name;
            VALUE = value;
        }
    }

    private static ParseResult<Integer> parseOffset(@NotNull CompilerData cd, boolean peekNext, @Nullable Token peekBlacklist, boolean addToProgram) {

        String token = peekNext ? (peekBlacklist == null ? cd.tokenizer.peekNext() : cd.tokenizer.peekNext(peekBlacklist)) : cd.tokenizer.getLast();
        if (token == null) return new ParseResult<>(PARSE_STATUS.FAIL);

        if (Tokens.OFF_START.equals(token)) {
            if (peekNext) cd.tokenizer.consumeNext(Tokens.SPACE);
            String offsetToken = cd.tokenizer.consumeNext(Tokens.SPACE);

            ParseResult<Integer> lastResult = parseNumber(cd, false);
            if (lastResult.STATUS == PARSE_STATUS.FAIL) lastResult = parseConstant(cd, true, false);
            if (lastResult.STATUS == PARSE_STATUS.FAIL) throw new SyntaxError("Constant or Number", offsetToken, cd.tokenizer);

            int offset = lastResult.VALUE;

            String offEndToken = cd.tokenizer.consumeNext(Tokens.SPACE);
            if (Tokens.OFF_END.equals(offEndToken)) {
                if (addToProgram) {
                    int address = cd.program.size() + offset;
                    cd.program.add(address);
                    return new ParseResult<>(PARSE_STATUS.SUCCESS, null, address);
                } else return new ParseResult<>(PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, null, offset);
            } else throw new SyntaxError("Offset terminator ('" + Tokens.OFF_END.PATTERN + "')", offEndToken, cd.tokenizer);
        } else return new ParseResult<>(PARSE_STATUS.FAIL);
    }

    private static ParseResult<Integer> parseLabel(@NotNull CompilerData cd, boolean declareOnly) {
        String lastToken = cd.tokenizer.getLast();
        if (lastToken == null) return new ParseResult<>(PARSE_STATUS.FAIL);

        String nextToken = cd.tokenizer.peekNext(Tokens.SPACE);
        if (Tokens.LABEL.equals(nextToken)) {
            // If a label is being declared consume the declaration token
            cd.tokenizer.consumeNext(Tokens.SPACE);
            if (cd.labels.containsKey(lastToken)) {
                // If a label was already created check if it has a pointer
                LabelData labelData = cd.labels.get(lastToken);
                if (labelData.pointer == LabelData.NULL_PTR)
                    labelData.pointer = cd.program.size();
                else
                    // If the label has a valid pointer then it was already declared!
                    throw new TypeError("Label '" + lastToken + "' was already declared", cd.tokenizer);
            } else
                // If no label was created then create it
                cd.labels.put(lastToken, new LabelData(cd.program.size()));
            return new ParseResult<>(PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, lastToken, cd.program.size());
        } else if (!declareOnly) {
            int offset = 0;
            ParseResult<Integer> offsetResult = parseOffset(cd, true, null, false);
            if (offsetResult.STATUS != PARSE_STATUS.FAIL) offset = offsetResult.VALUE;

            if (cd.labels.containsKey(lastToken)) {
                LabelData labelData = cd.labels.get(lastToken);
                labelData.addOccurrence(cd.program.size(), offset);
            } else {
                LabelData labelData = new LabelData();
                labelData.addOccurrence(cd.program.size(), offset);
                cd.labels.put(lastToken, labelData);
            }
            cd.program.add(0);
            return new ParseResult<>(PARSE_STATUS.SUCCESS, lastToken, cd.program.size());
        } else return new ParseResult<>(PARSE_STATUS.FAIL);
    }

    private static ParseResult<Integer> parseConstant(@NotNull CompilerData cd, boolean isGetting, boolean addToProgram) {
        String lastToken = cd.tokenizer.getLast();
        if (lastToken == null) return new ParseResult<>(PARSE_STATUS.FAIL);

        if (Tokens.CONSTANT.equals(lastToken)) {
            String constantName = cd.tokenizer.consumeNext(Tokens.SPACE);
            if (isGetting) {
                if (constantName == null) throw new SyntaxError("Constant's name", "null", cd.tokenizer);
                else if (cd.constants.containsKey(constantName)) {
                    // If the constant was defined save its value
                    if (addToProgram) cd.program.add(cd.constants.get(constantName));
                    return new ParseResult<>(addToProgram ? PARSE_STATUS.SUCCESS : PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, constantName, cd.constants.get(constantName));
                } else throw new ReferenceError("Constant", constantName, cd.tokenizer);
            } else {
                String constantValue = cd.tokenizer.consumeNext(Tokens.SPACE);
                if (constantName == null) throw new SyntaxError("Constant's name", "null", cd.tokenizer);
                else if (constantValue == null) throw new SyntaxError("Number, Character or Constant", "null", cd.tokenizer);

                ParseResult<Integer> result = parseNumber(cd, false);
                if (result.STATUS == PARSE_STATUS.FAIL) result = parseCharacter(cd, false);
                if (result.STATUS == PARSE_STATUS.FAIL) result = parseConstant(cd, true, false);
                if (result.STATUS == PARSE_STATUS.FAIL)
                    throw new SyntaxError("Number, Character or Constant", constantValue, cd.tokenizer);

                cd.constants.put(constantName, result.VALUE);
                return new ParseResult<>(PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, constantName, result.VALUE);
            }
        } else return new ParseResult<>(PARSE_STATUS.FAIL);
    }

    private static ParseResult<Integer> parseNumber(@NotNull CompilerData cd, boolean addToProgram) {
        String lastToken = cd.tokenizer.getLast();
        if (lastToken == null) return new ParseResult<>(PARSE_STATUS.FAIL);

        try {
            int value = StringUtils.parseInt(lastToken);
            if (addToProgram) cd.program.add(value);
            return new ParseResult<>(addToProgram ? PARSE_STATUS.SUCCESS : PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, null, value);
        } catch (Exception err) {
            return new ParseResult<>(PARSE_STATUS.FAIL);
        }
    }

    private static ParseResult<String> parseString(@NotNull CompilerData cd, boolean addToProgram) {
        String terminator = cd.tokenizer.getLast();
        if (terminator == null) return new ParseResult<>(PARSE_STATUS.FAIL);

        if (Tokens.STRING.equals(terminator)) {
            boolean escapeChar = false;
            StringBuilder value = new StringBuilder();
            while (true) {
                String valueToAdd = cd.tokenizer.consumeNext();
                if (valueToAdd == null)
                    throw new SyntaxError("String terminator ('" + terminator + "')", String.valueOf(value.charAt(value.length() - 1)), cd.tokenizer);
                else if (escapeChar) {
                    char escapedChar = valueToAdd.charAt(0);
                    if (StringUtils.SpecialCharacters.MAP.containsKey(escapedChar)) {
                        value.append(StringUtils.SpecialCharacters.MAP.get(escapedChar));
                        if (valueToAdd.length() > 1)
                            value.append(valueToAdd.substring(1));
                    } else value.append(valueToAdd);
                    escapeChar = false;
                } else if (valueToAdd.equals(terminator)) break;
                else if (Tokens.ESCAPE_CH.equals(valueToAdd)) escapeChar = true;
                else value.append(valueToAdd);
            }
            if (addToProgram) {
                for (int i = 0; i < value.length(); i++) cd.program.add((int) value.charAt(i));
                return new ParseResult<>(PARSE_STATUS.SUCCESS, null, value.toString());
            } else return new ParseResult<>(PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, null, value.toString());
        } else return new ParseResult<>(PARSE_STATUS.FAIL);
    }

    private static ParseResult<Integer> parseCharacter(@NotNull CompilerData cd, boolean addToProgram) {
        String terminator = cd.tokenizer.getLast();
        if (terminator == null) return new ParseResult<>(PARSE_STATUS.FAIL);

        if (Tokens.CHARACTER.equals(terminator)) {
            char character;

            String nextToken = cd.tokenizer.consumeNext();
            if (nextToken == null || terminator.equals(nextToken) || nextToken.length() > 1)
                throw new SyntaxError("Character", nextToken, cd.tokenizer);
            else if (Tokens.ESCAPE_CH.equals(nextToken)) {
                String escapedChar = cd.tokenizer.consumeNext();
                if (escapedChar == null || escapedChar.length() > 1)
                    throw new SyntaxError("Character", escapedChar, cd.tokenizer);
                character = StringUtils.SpecialCharacters.MAP.getOrDefault(escapedChar.charAt(0), escapedChar.charAt(0));
            } else character = nextToken.charAt(0);

            if (!terminator.equals(cd.tokenizer.consumeNext()))
                throw new SyntaxError("Character terminator ('" + terminator + "')", nextToken, cd.tokenizer);

            if (addToProgram) {
                cd.program.add((int) character);
                return new ParseResult<>(PARSE_STATUS.SUCCESS, null, (int) character);
            } else return new ParseResult<>(PARSE_STATUS.SUCCESS_PROGRAM_NOT_CHANGED, null, (int) character);
        } else return new ParseResult<>(PARSE_STATUS.FAIL);
    }

    private static ParseResult<Integer> parseValues(@NotNull CompilerData cd) {
        // Get the value to parse
        String valueToParse = cd.tokenizer.consumeNext(Tokens.SPACE);
        // Throw if there's no value, because there MUST be one if this function is called
        if (valueToParse == null) throw new SyntaxError("Number, Char, Offset, Constant or Label", "null", cd.tokenizer);

        ParseResult<Integer> lastResult = parseNumber(cd, true);
        if (lastResult.STATUS == PARSE_STATUS.FAIL) lastResult = parseCharacter(cd, true);
        if (lastResult.STATUS == PARSE_STATUS.FAIL) lastResult = parseOffset(cd, false, null, true);
        if (lastResult.STATUS == PARSE_STATUS.FAIL) lastResult = parseConstant(cd, true, true);
        if (lastResult.STATUS == PARSE_STATUS.FAIL) lastResult = parseLabel(cd, false);

        if (lastResult.STATUS == PARSE_STATUS.FAIL)
            throw new SyntaxError("Number, Char, Offset, Constant or Label", valueToParse, cd.tokenizer);
        return lastResult;
    }

    public static int[] compileFile(@NotNull File file, @NotNull InstructionSet instructionSet) {
        if (!file.exists()) throw new IllegalArgumentException("'" + file.getAbsolutePath() + "': The specified file doesn't exist.");
        if (!file.canRead()) throw new IllegalArgumentException("'" + file.getAbsolutePath() + "': The specified file can't be read.");

        String contents;
        try {
            contents = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);;
        } catch (Exception err) {
            throw new IllegalStateException("Something went wrong while reading the specified file.");
        }

        CompilerData cd = new CompilerData();
        cd.tokenizer = new Tokenizer(contents, true, Tokens.ALL_TOKENS);
        cd.tokenizer.removeEmpties();

        while (cd.tokenizer.hasNext()) {
            String token = cd.tokenizer.consumeNext(Tokens.SPACE);
            if (token == null || Tokens.COMMENT.equals(token)) break;

            int instructionCode = instructionSet.getKeyCode(token);
            Instruction instruction = instructionSet.getInstruction(instructionCode);

            if (instruction != null) {
                // If an instruction was found add it to the memory
                cd.program.add(instructionCode);
                for (int i = 0; i < instruction.ARGUMENTS;)
                    if (parseValues(cd).STATUS == PARSE_STATUS.SUCCESS) i++;

            } else if (Tokens.COMPILER.equals(token)) {
                // Parsing Compiler Instructions
                String compilerInstr = cd.tokenizer.consumeNext(Tokens.SPACE);
                if (compilerInstr == null)
                    throw new SyntaxError("Compiler Instruction", "null", cd.tokenizer);
                else if (compilerInstr.equals("DW")) {
                    parseValues(cd);
                } else if (compilerInstr.equals("DS")) {
                    cd.tokenizer.consumeNext(Tokens.SPACE);
                    if (parseString(cd, true).STATUS == PARSE_STATUS.FAIL)
                        throw new SyntaxError("String", cd.tokenizer.getLast(), cd.tokenizer);
                } else if (compilerInstr.equals("DA")) {
                    // Be sure that there's the character that starts the array
                    String arrayStart = cd.tokenizer.consumeNext(Tokens.SPACE);
                    if (Tokens.ARR_START.equals(arrayStart)) {
                        while (true) {
                            // For each value in the array, check if the next value is the array closer character
                            String nextToken = cd.tokenizer.peekNext(Tokens.SPACE);
                            if (Tokens.ARR_END.equals(nextToken)) {
                                cd.tokenizer.consumeNext(Tokens.SPACE);
                                break;
                            }

                            try {
                                parseValues(cd);
                            } catch (Exception err) {
                                throw new SyntaxError("Array terminator ('" + Tokens.ARR_END.PATTERN + "')", cd.tokenizer.getLast(), cd.tokenizer);
                            }
                        }
                    } else throw new SyntaxError("Array", arrayStart, cd.tokenizer);
                } else throw new SyntaxError("Compiler Instruction", compilerInstr, cd.tokenizer);
            } else {
                // Parsing Labels and Constants
                if (parseConstant(cd, false, true).STATUS == PARSE_STATUS.FAIL && parseLabel(cd, true).STATUS == PARSE_STATUS.FAIL)
                    throw new SyntaxError("Constant or Label declaration", cd.tokenizer.getLast(), cd.tokenizer);
            }
        }

        cd.labels.forEach((key, data) -> {
            if (data.pointer == LabelData.NULL_PTR) throw new ReferenceError("Label", key, -1, -1);
            if (data.occurrences.size() != data.offsets.size()) throw new IllegalStateException("Label '" + key + "' has different amounts of occurrences and offsets.");
            for (int i = 0; i < data.occurrences.size(); i++)
                cd.program.set(data.occurrences.get(i), data.pointer + data.offsets.get(i));
        });

        int[] primitiveIntProgram = new int[cd.program.size()];
        for (int i = 0; i < cd.program.size(); i++)
            primitiveIntProgram[i] = cd.program.get(i);
        return primitiveIntProgram;
    }

    public static @NotNull String obfuscateProgram(int[] program) {
        switch (program.length) {
            case 0: return "; Nothing to see here ;)";
            case 1: return "#DW " + program[0];
            default: {
                StringBuilder obfProgram = new StringBuilder();
                obfProgram.append("#DA { ");
                for (int value : program) {
                    obfProgram.append(value).append(' ');
                }
                obfProgram.append("}");
                return obfProgram.toString();
            }
        }
    }

}
