package io.github.hds.pemu.compiler.parser;

import io.github.hds.pemu.compiler.CompilerVars;
import io.github.hds.pemu.files.FileUtils;
import io.github.hds.pemu.instructions.Instruction;
import io.github.hds.pemu.instructions.InstructionSet;
import io.github.hds.pemu.memory.flags.IFlag;
import io.github.hds.pemu.memory.flags.IMemoryFlag;
import io.github.hds.pemu.memory.registers.IMemoryRegister;
import io.github.hds.pemu.memory.registers.IRegister;
import io.github.hds.pemu.processor.IProcessor;
import io.github.hds.pemu.tokenizer.Token;
import io.github.hds.pemu.tokenizer.TokenDefinition;
import io.github.hds.pemu.tokenizer.Tokenizer;
import io.github.hds.pemu.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class Parser {
    private static final char ESCAPE_CHAR = '\\';
    private static final char ESCAPE_TERM = ';';

    private static final String CI_DEFINE_WORD   = "DW";
    private static final String CI_DEFINE_STRING = "DS";
    private static final String CI_DEFINE_ARRAY  = "DA";
    private static final String CI_INCLUDE       = "INCLUDE";

    private static final String STATIC_TYPES = "Number, Character, Compiler Variable or Register";
    private static final String ARGUMENT_TYPES = "Label, Offset, " + STATIC_TYPES;
    private static final String GENERIC_TYPES = "Instruction, Compiler Instruction, Compiler Variable or Label";

    private static final TokenDefinition COMMENT    = new TokenDefinition("Comment", ";[^\\v]*\\v?");
    private static final TokenDefinition LABEL_DECL = new TokenDefinition("Label Declaration", ":", true);
    private static final TokenDefinition HEX_NUMBER = new TokenDefinition("Hex Number", "0x[0-9a-fA-F]+");
    private static final TokenDefinition OCT_NUMBER = new TokenDefinition("Octal Number", "0o[0-7]+");
    private static final TokenDefinition BIN_NUMBER = new TokenDefinition("Binary Number", "0b[01]+");
    private static final TokenDefinition DEC_NUMBER = new TokenDefinition("Decimal Number", "[+\\-]?[0-9]+");
    private static final TokenDefinition STRING     = new TokenDefinition("String", "\"((?:[^\\\\\"]|(?:\\\\[0-9]+;?|\\\\.))*)\"");
    private static final TokenDefinition CHARACTER  = new TokenDefinition("Character", "'(\\\\.|\\\\[0-9]+;?|[^\\\\])'");
    private static final TokenDefinition C_VAR      = new TokenDefinition("Compiler Variable", "@([_A-Z][_A-Z0-9]*)", false, true);
    private static final TokenDefinition C_INSTR    = new TokenDefinition("Compiler Instruction", "#(DW|DS|DA|INCLUDE)");
    private static final TokenDefinition L_BRACE    = new TokenDefinition("Left Brace", "{", true);
    private static final TokenDefinition R_BRACE    = new TokenDefinition("Right Braces", "}", true);
    private static final TokenDefinition L_BRACKET  = new TokenDefinition("Left Bracket", "[", true);
    private static final TokenDefinition R_BRACKET  = new TokenDefinition("Right Bracket", "]", true);
    private static final TokenDefinition IDENTIFIER = new TokenDefinition("Identifier", "[_A-Z][_A-Z0-9]*", false, true);
    private static final TokenDefinition SPACE      = new TokenDefinition("Space", "\\h+");
    private static final TokenDefinition NEWLINE    = new TokenDefinition("New Line", "\\v+");

    private static final TokenDefinition[] ALL_DEFINITIONS = new TokenDefinition[] {
            LABEL_DECL, COMMENT,
            HEX_NUMBER, OCT_NUMBER, BIN_NUMBER, DEC_NUMBER,
            STRING, CHARACTER,
            C_VAR, C_INSTR,
            L_BRACE, R_BRACE,
            L_BRACKET, R_BRACKET,
            IDENTIFIER,
            SPACE, NEWLINE
    };

    private static @NotNull String formatToken(@Nullable Token token) {
        return token == null ? "EOF" : token.getMatch();
    }

    private static class ParseResult<T> {
        public final boolean SUCCESS;
        public final T VALUE;

        protected ParseResult() {
            this(false, null);
        }

        protected ParseResult(boolean success, T value) {
            SUCCESS = success;
            VALUE = value;
        }
    }

    private static @NotNull ParseResult<IValueProvider> parseNumber(@NotNull ParserContext ctx, boolean addNodes) {
        Token currentToken = ctx.tokenizer.getCurrentToken();

        int value;
        if (
                HEX_NUMBER.isDefinitionOf(currentToken) ||
                OCT_NUMBER.isDefinitionOf(currentToken) ||
                BIN_NUMBER.isDefinitionOf(currentToken) ||
                DEC_NUMBER.isDefinitionOf(currentToken)
        ) {
            assert currentToken != null;
            value = StringUtils.parseInt(currentToken.getMatch());
        } else return new ParseResult<>();

        IValueProvider valueProvider = () -> value;
        if (addNodes) ctx.addNode(new ValueNode(valueProvider));

        return new ParseResult<>(true, valueProvider);
    }

    private static char strCodePointToChar(@NotNull ParserContext ctx, @NotNull String str) {
        boolean isCodePointValid = false;

        int codePoint = 0;
        try {
            codePoint = Integer.parseUnsignedInt(str);
            isCodePointValid = Character.isValidCodePoint(codePoint);
        } catch (Exception ignored) { }

        if (!isCodePointValid)
            throw new ParserError.SyntaxError(ctx, "Char Code Point", str);

        return (char) codePoint;
    }

    private static @NotNull String unescapeString(@NotNull ParserContext ctx, @NotNull String str) {
        StringBuilder strBuilder = new StringBuilder(str.length());

        boolean isEscaping = false;
        StringBuilder codePointBuilder = new StringBuilder(4);

        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (isEscaping) {
                if (Character.isDigit(currentChar)) {
                    codePointBuilder.append(currentChar);
                } else if (codePointBuilder.length() > 0) {
                    strBuilder.append(
                            strCodePointToChar(ctx, codePointBuilder.toString())
                    );
                    codePointBuilder.setLength(0);

                    if (currentChar != ESCAPE_TERM)
                        strBuilder.append(currentChar);
                    isEscaping = false;
                } else {
                    strBuilder.append(
                        StringUtils.SpecialCharacters.MAP.getOrDefault(currentChar, currentChar)
                    );
                    isEscaping = false;
                }
            } else if (currentChar == ESCAPE_CHAR)
                isEscaping = true;
            else strBuilder.append(currentChar);
        }

        if (codePointBuilder.length() > 0) {
            strBuilder.append(
                    strCodePointToChar(ctx, codePointBuilder.toString())
            );
        }

        return strBuilder.toString();
    }

    private static @NotNull ParseResult<IValueProvider> parseCharacter(@NotNull ParserContext ctx, boolean addNodes) {
        Token currentToken = ctx.tokenizer.getCurrentToken();

        int value;
        if (CHARACTER.isDefinitionOf(currentToken)) {
            assert currentToken != null;

            String rawChar = currentToken.getGroups()[0];
            String character = unescapeString(ctx, rawChar);

            if (character.length() != 1)
                throw new ParserError.SyntaxError(ctx, CHARACTER.getName(), rawChar);
            value = character.charAt(0);
        } else return new ParseResult<>();

        IValueProvider valueProvider = () -> value;
        if (addNodes) ctx.addNode(new ValueNode(valueProvider));

        return new ParseResult<>(true, valueProvider);
    }

    private static @NotNull ParseResult<String> parseString(@NotNull ParserContext ctx, boolean addNodes) {
        Token currentToken = ctx.tokenizer.getCurrentToken();
        if (!STRING.isDefinitionOf(currentToken))
            return new ParseResult<>();
        assert currentToken != null;

        String str = unescapeString(ctx, currentToken.getGroups()[0]);
        if (addNodes) ctx.addNode(new StringNode(str));

        return new ParseResult<>(true, str);
    }

    private static @NotNull ParseResult<IValueProvider> parseRegister(@NotNull ParserContext ctx, boolean addNodes) {
        Token currentToken = ctx.tokenizer.getCurrentToken();
        if (!IDENTIFIER.isDefinitionOf(currentToken))
            return new ParseResult<>();
        assert currentToken != null;

        String regName = currentToken.getMatch();
        int address = -1;

        IRegister register = ctx.processor.getRegister(regName);
        if (register == null) {
            IFlag flag = ctx.processor.getFlag(regName);
            if (flag != null) {
                if (!(flag instanceof IMemoryFlag))
                    throw new ParserError.ProcessorError(ctx, "Reading/Writing to Flag \"" + regName + "\" isn't supported!");
                address = ((IMemoryFlag) flag).getAddress();
            }
        } else {
            if (!(register instanceof IMemoryRegister))
                throw new ParserError.ProcessorError(ctx, "Reading/Writing to Register \"" + regName + "\" isn't supported!");
            address = ((IMemoryRegister) register).getAddress();
        }

        if (address < 0) return new ParseResult<>();

        int finalAddress = address; // IntelliJ would get mad at me if I didn't put this here, don't know why tho
        IValueProvider valueProvider = () -> finalAddress;
        if (addNodes) ctx.addNode(new ValueNode(valueProvider));
        return new ParseResult<>(true, valueProvider);
    }

    private static @NotNull ParseResult<IValueProvider> parseCompilerVar(@NotNull ParserContext ctx, boolean isGetting, boolean addNodes) {
        Token currentToken = ctx.tokenizer.getCurrentToken();
        if (!C_VAR.isDefinitionOf(currentToken))
            return new ParseResult<>();
        assert currentToken != null;

        String varName = currentToken.getGroups()[0];
        if (isGetting) {
            if (!ctx.hasCompilerVar(varName))
                throw new ParserError.ReferenceError(ctx, C_VAR.getName(), varName, "was not declared.");

            IValueProvider valueProvider = () -> ctx.getCompilerVar(varName);
            if (addNodes) ctx.addNode(new ValueNode(valueProvider));
            return new ParseResult<>(true, valueProvider);
        }

        Token staticValToken = ctx.tokenizer.goForward();

        ParseResult<IValueProvider> staticValue = parseStaticValue(ctx, false);
        if (!staticValue.SUCCESS)
            throw new ParserError.SyntaxError(ctx, STATIC_TYPES, formatToken(staticValToken));

        ctx.putCompilerVar(varName, staticValue.VALUE.getValue());
        return new ParseResult<>(true, staticValue.VALUE);
    }

    private static @NotNull ParseResult<IValueProvider> parseStaticValue(@NotNull ParserContext ctx, boolean addNodes) {
        ParseResult<IValueProvider> result;

        result = parseNumber(ctx, addNodes);
        if (!result.SUCCESS) result = parseCharacter(ctx, addNodes);
        if (!result.SUCCESS) result = parseCompilerVar(ctx, true, addNodes);
        if (!result.SUCCESS) result = parseRegister(ctx, addNodes);

        return result;
    }

    /**
     *
     * @param ctx
     * @return
     */
    private static int parseArgument(@NotNull ParserContext ctx, boolean allowLabelDeclaration) {
        int labelParseResult = parseLabel(ctx, allowLabelDeclaration, true);
        if (labelParseResult > 0) return 1;
        else if (labelParseResult == 0) return 0;

        if (parseOffset(ctx, true).SUCCESS) return 1;
        return parseStaticValue(ctx, true).SUCCESS ? 1 : -1;
    }

    private static @NotNull ParseResult<IValueProvider> parseOffset(@NotNull ParserContext ctx, boolean addNodes) {
        Token currentToken = ctx.tokenizer.getCurrentToken();
        if (!L_BRACKET.isDefinitionOf(currentToken))
            return new ParseResult<>();

        Token staticValToken = ctx.tokenizer.goForward();
        ParseResult<IValueProvider> staticValue = parseStaticValue(ctx, false);
        if (!staticValue.SUCCESS)
            throw new ParserError.SyntaxError(ctx, STATIC_TYPES, formatToken(staticValToken));
        if (addNodes) ctx.addNode(new OffsetNode(staticValue.VALUE));

        currentToken = ctx.tokenizer.goForward();
        if (!R_BRACKET.isDefinitionOf(currentToken))
            return new ParseResult<>();

        return new ParseResult<>(true, staticValue.VALUE);
    }

    private static boolean parseArray(@NotNull ParserContext ctx) {
        ParseResult<IValueProvider> arraySize = parseOffset(ctx, false);
        if (arraySize.SUCCESS) {
            ctx.addNode(new ArrayNode(arraySize.VALUE));
            return true;
        }

        if (!L_BRACE.isDefinitionOf(ctx.tokenizer.getCurrentToken()))
            return false;

        do {
            ctx.tokenizer.goForward();
        } while (parseArgument(ctx, true) >= 0);

        Token arrayEndToken = ctx.tokenizer.getCurrentToken();
        if (!R_BRACE.isDefinitionOf(ctx.tokenizer.getCurrentToken()))
            throw new ParserError.SyntaxError(ctx, "Array Definition", formatToken(arrayEndToken));
        return true;
    }

    /**
     * Parses a Label<br>
     * NOTE: THIS ALWAYS ADDS A NODE
     * @param ctx The {@link ParserContext}
     * @param canDeclare Whether or not a Label can be Declared
     * @param canUse Whether or not a Label can be Used
     * @return -1 if parsing failed, 0 if a Label was Declared, 1 if it was used
     */
    private static int parseLabel(@NotNull ParserContext ctx, boolean canDeclare, boolean canUse) {
        Token currentToken = ctx.tokenizer.getCurrentToken();
        if (!IDENTIFIER.isDefinitionOf(currentToken))
            return -1;
        assert currentToken != null;

        String labelName = currentToken.getMatch();
        boolean isDeclaration =
                LABEL_DECL.isDefinitionOf(ctx.tokenizer.peekForward());
        IValueProvider offset = () -> 0;

        if (isDeclaration) {
            ctx.tokenizer.goForward();
            if (!canDeclare)
                throw new ParserError.TypeError(ctx, "Label Declaration isn't allowed here.");
        } else if (!canUse) {
            throw new ParserError.TypeError(ctx, "Label Usage isn't allowed here.");
        } else if (L_BRACKET.isDefinitionOf(ctx.tokenizer.peekForward())) {
            Token offsetToken = ctx.tokenizer.goForward();
            ParseResult<IValueProvider> result = parseOffset(ctx, false);

            // This should never throw
            if (!result.SUCCESS)
                throw new ParserError.SyntaxError(ctx, "Offset", formatToken(offsetToken));

            offset = result.VALUE;
        }

        ctx.addNode(
                new LabelNode(ctx, labelName, offset, isDeclaration)
        );

        return isDeclaration ? 0 : 1;
    }

    private static @NotNull ParseResult<Instruction> parseInstruction(@NotNull ParserContext ctx) {
        Token currentToken = ctx.tokenizer.getCurrentToken();
        if (!IDENTIFIER.isDefinitionOf(currentToken))
            return new ParseResult<>();
        assert currentToken != null;

        InstructionSet instructionSet = ctx.processor.getInstructionSet();
        int keyCode = instructionSet.getKeyCode(
                currentToken.getMatch()
        );

        if (keyCode < 0)
            return new ParseResult<>();

        Instruction instruction = instructionSet.getInstruction(keyCode);
        assert instruction != null;

        ctx.addNode(new ValueNode(keyCode));
        for (int i = 0; i < instruction.getArgumentsCount();) {
            Token currentArgToken = ctx.tokenizer.goForward();

            int parseArgumentResult = parseArgument(ctx, true);
            if (parseArgumentResult > 0) i++;
            else if (parseArgumentResult < 0)
                throw new ParserError.ArgumentsError(ctx, instruction.getKeyword(), i, ARGUMENT_TYPES, formatToken(currentArgToken));
        }

        return new ParseResult<>(true, instruction);
    }

    private static @NotNull List<INode> internalParseFile(@NotNull IProcessor processor, @NotNull File srcFile, @NotNull HashSet<String> includedFiles, @NotNull CompilerVars cVars) {
        String srcFilePath = FileUtils.tryGetCanonicalPath(srcFile);
        if (includedFiles.contains(srcFilePath))
            return new ArrayList<>(0);
        includedFiles.add(srcFilePath);

        String src;
        try {
            src = new String(Files.readAllBytes(srcFile.toPath()), StandardCharsets.UTF_8);
        } catch (Exception err) {
            throw new ParserError.FileError(srcFile, "Something went wrong when reading file.");
        }

        ParserContext ctx = new ParserContext(
                processor, srcFile, new Tokenizer(src, ALL_DEFINITIONS), cVars
        );

        ctx.tokenizer.removeTokensByDefinition(
                COMMENT, NEWLINE, SPACE
        );

        while (ctx.tokenizer.canGoForward()) {
            Token currentToken = ctx.tokenizer.goForward();

            if (parseInstruction(ctx).SUCCESS)
                continue;

            if (C_INSTR.isDefinitionOf(currentToken)) {
                assert currentToken != null;
                String instrName = currentToken.getGroups()[0];

                Token instrArg = ctx.tokenizer.goForward();
                switch (instrName) {
                    case CI_DEFINE_WORD:
                        if (parseArgument(ctx, false) <= 0)
                            throw new ParserError.SyntaxError(ctx, ARGUMENT_TYPES, formatToken(instrArg));
                        break;
                    case CI_DEFINE_STRING:
                        if (!parseString(ctx, true).SUCCESS)
                            throw new ParserError.SyntaxError(ctx, STRING.getName(), formatToken(instrArg));
                        break;
                    case CI_DEFINE_ARRAY:
                        if (!parseArray(ctx))
                            throw new ParserError.SyntaxError(ctx, "Array", formatToken(instrArg));
                        break;
                    case CI_INCLUDE:
                        ParseResult<String> parsedPath = parseString(ctx, false);
                        if (!parsedPath.SUCCESS)
                            throw new ParserError.SyntaxError(ctx, "Include File Path", formatToken(instrArg));
                        String includePath = parsedPath.VALUE;

                        File includeFile;
                        try {
                            includeFile = new File(srcFile.toURI().resolve(includePath));
                        } catch (Exception err) {
                            throw new ParserError.SyntaxError(ctx, "Valid Include Path", includePath);
                        }

                        if (!includeFile.exists())
                            throw new ParserError.FileError(ctx, "Couldn't include file because it doesn't exist");
                        if (!includeFile.canRead())
                            throw new ParserError.FileError(ctx, "Couldn't include file because it can't be read");

                        ctx.addNodes(
                                internalParseFile(ctx.processor, includeFile, includedFiles, cVars)
                        );
                        break;
                    default:
                        // This should never throw
                        throw new ParserError.SyntaxError(ctx, C_INSTR.getName(), instrName);
                }

                continue;
            }

            if (
                    !parseCompilerVar(ctx, false, false).SUCCESS &&
                    parseLabel(ctx, true, false) != 0
            ) throw new ParserError.SyntaxError(ctx, GENERIC_TYPES, formatToken(currentToken));
        }

        return ctx.getNodes();
    }

    public static @NotNull List<INode> parseFile(@NotNull File srcFile, @NotNull IProcessor processor) {
        if (!srcFile.exists())
            throw new ParserError.FileError(srcFile, "Couldn't compile file because it doesn't exist");
        if (!srcFile.canRead())
            throw new ParserError.FileError(srcFile, "Couldn't compile file because it can't be read");
        return Parser.internalParseFile(processor, srcFile, new HashSet<>(), CompilerVars.getDefaultVars());
    }
}