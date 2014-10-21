/**
 * Copyright 2014 Dreamhunters-net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nitorcreations.robotframework.eclipseide.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

public class RobotFrameworkAutoEditStrategy implements IAutoEditStrategy {
    private boolean bracketOpen = true;
    private boolean stringOpen = true;
    private boolean quoteOpen = true;

    private void enableBracketOpen() {
        bracketOpen = false;
    }

    private void disableBracketOpen() {
        bracketOpen = true;
    }

    private void enableStringOpen() {
        stringOpen = false;
    }

    private void disableStringOpen() {
        stringOpen = true;
    }

    private void enableQuoteOpen() {
        quoteOpen = false;
    }

    private void disableQuoteOpen() {
        quoteOpen = true;
    }

    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        int line;
        try {
            line = document.getLineOfOffset(command.offset);
        } catch (BadLocationException e) {
            line = -1;
        }
        try {
            if (command.text.equals("\"")) {
                if (detectOpenStrings(document, line) && (!stringOpen) && (detectNextChar(document, command) == '\"')) {
                    command.text = "";
                    configureCommandFwd(command);
                    disableStringOpen();
                } else {
                    command.text = "\"\"";
                    configureCommandBack(command);
                    enableStringOpen();
                }
            } else if (command.text.equals("'")) {
                if (detectCloseQuote(document, line) && (!quoteOpen) && (detectNextChar(document, command) == '\'')) {
                    command.text = "";
                    configureCommandFwd(command);
                    disableQuoteOpen();
                } else {
                    command.text = "''";
                    configureCommandBack(command);
                    enableQuoteOpen();
                }
            } else if (command.text.equals("{")) {
                command.text = "{}";
                configureCommandBack(command);
                enableBracketOpen();
            } else if (command.text.equals("}")) {
                if (detectOpenBrackets(document, line) && (!bracketOpen) && (detectNextChar(document, command) == '}')) {
                    command.text = "";
                    configureCommandFwd(command);
                    disableBracketOpen();
                }
            } else if (command.text.equals("(")) {
                command.text = "()";
                configureCommandBack(command);
                enableBracketOpen();
            } else if (command.text.equals(")")) {
                if (detectOpenBrackets(document, line) && (!bracketOpen) && (detectNextChar(document, command) == ')')) {
                    command.text = "";
                    configureCommandFwd(command);
                    disableBracketOpen();
                }
            } else if (command.text.equals("[")) {
                command.text = "[]";
                configureCommandBack(command);
                enableBracketOpen();
            } else if (command.text.equals("]")) {
                if (detectOpenBrackets(document, line) && (!bracketOpen) && (detectNextChar(document, command) == ']')) {
                    command.text = "";
                    configureCommandFwd(command);
                    disableBracketOpen();
                }
            }
        } catch (BadLocationException e) {}
    }

    private char detectNextChar(IDocument document, DocumentCommand command) throws BadLocationException {
        char c = document.getChar(command.offset);
        return c;
    }

    private void configureCommandBack(DocumentCommand command) {
        // puts the caret between both the quotes

        command.caretOffset = command.offset + 1;
        command.shiftsCaret = false;
    }

    private void configureCommandFwd(DocumentCommand command) {
        // puts the caret between both the quotes

        command.caretOffset = command.offset + 1;
        command.shiftsCaret = false;
    }

    private boolean detectOpenBrackets(IDocument document, int line) throws BadLocationException {
        boolean ret = false;
        if (line > -1) {
            int openBracketCount = 0;
            int start;
            start = document.getLineOffset(line);
            int end = start + document.getLineLength(line) - 1;
            while (start < end) {
                char c = document.getChar(start);
                if ((c == '{') || (c == '[') || (c == '(')) {
                    openBracketCount++;
                } else if ((c == '}') || (c == ']') || (c == ')')) {
                    openBracketCount++;
                }
                start++;
            }
            int openBracketCountMod = openBracketCount % 2;
            if ((openBracketCountMod != 0) || (!bracketOpen)) {
                if (openBracketCount != 0) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    private boolean detectOpenStrings(IDocument document, int line) throws BadLocationException {
        boolean ret = false;
        if (line > -1) {
            int openQuoteCount = 0;
            int start = document.getLineOfOffset(line);
            int end = start + document.getLineLength(line) - 1;
            while (start < end) {
                char c = document.getChar(start);
                if (c == '\"') {
                    openQuoteCount++;
                }
                start++;
            }
            int openQuoteCountMod = openQuoteCount % 2;
            if ((openQuoteCountMod != 0) || (openQuoteCount == 0)) {
                ret = true;
            }
        }
        return ret;
    }

    private boolean detectCloseQuote(IDocument document, int line) throws BadLocationException {
        boolean ret = false;
        if (line > -1) {
            int openQuoteCount = 0;
            int start = document.getLineOfOffset(line);
            int end = start + document.getLineLength(line) - 1;
            while (start < end) {
                char c = document.getChar(start);
                if (c == '\'') {
                    openQuoteCount++;
                }
                start++;
            }
            int openQuoteCountMod = openQuoteCount % 2;
            if ((openQuoteCountMod != 0) || (openQuoteCount == 0)) {
                ret = true;
            }
        }
        return ret;
    }

}
