/**
 * Copyright 2012 Nitor Creations Oy
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
package com.nitorcreations.robotframework.eclipseide.internal.hyperlinks;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.nitorcreations.robotframework.eclipseide.builder.parser.RFELine;
import com.nitorcreations.robotframework.eclipseide.builder.parser.RFELine.LineType;
import com.nitorcreations.robotframework.eclipseide.internal.hyperlinks.util.KeywordInlineArgumentMatcher;
import com.nitorcreations.robotframework.eclipseide.internal.hyperlinks.util.KeywordInlineArgumentMatcher.KeywordMatchResult;
import com.nitorcreations.robotframework.eclipseide.internal.rules.RFTArgumentUtils;
import com.nitorcreations.robotframework.eclipseide.internal.util.BaseDefinitionMatchVisitor;
import com.nitorcreations.robotframework.eclipseide.internal.util.DefinitionFinder;
import com.nitorcreations.robotframework.eclipseide.structure.ParsedString;
import com.nitorcreations.robotframework.eclipseide.structure.ParsedString.ArgumentType;

/**
 * This hyperlink detector creates hyperlinks for keyword calls, e.g. *
 * <ul>
 * <li><tt>SomeKeyword &nbsp;${variable}</tt> - "SomeKeyword" is linked)</li>
 * <li><tt>Some ${inlinearg} testcase</tt> - "Some ${inlinearg} testcase" is
 * linked</li>
 * <li><tt>[Arguments] &nbsp;${foo}</tt> - "${foo}" is linked</li>
 * </ul>
 */
public class KeywordCallHyperlinkDetector extends HyperlinkDetector {

    private static final class KeywordMatchVisitor extends BaseDefinitionMatchVisitor {
        private final IRegion linkRegion;
        private final String linkString;
        private final List<IHyperlink> links;

        KeywordMatchVisitor(String linkString, IRegion linkRegion, IFile file, List<IHyperlink> links) {
            super(file);
            this.linkRegion = linkRegion;
            this.linkString = linkString;
            this.links = links;
        }

        @Override
        public VisitorInterest visitMatch(ParsedString match, IFile location) {
            String matchString = getMatchStringInFile(location, linkString);
            KeywordMatchResult matchResult = KeywordInlineArgumentMatcher.match(match.getValue().toLowerCase(), matchString);
            if (matchResult != KeywordMatchResult.DIFFERENT) {
                IRegion targetRegion = new Region(match.getArgEndCharPos(), 0);
                links.add(new Hyperlink(linkRegion, getFilePrefix(location) + match.getValue(), targetRegion, location));
                return location == file ? VisitorInterest.CONTINUE_TO_END_OF_CURRENT_FILE : VisitorInterest.CONTINUE;
            }
            return VisitorInterest.CONTINUE;
        }

        private String getMatchStringInFile(IFile location, String linkString) {
            String filePrefix = getNameWithoutTxtPostfix(location.getName()) + ".";
            if (linkString.startsWith(filePrefix)) {
                return linkString.substring(filePrefix.length()).toLowerCase();
            }
            return linkString.toLowerCase();
        }
    }

    @Override
    protected void getLinks(IFile file, RFELine rfeLine, ParsedString argument, int offset, List<IHyperlink> links) {
        if (argument.getType() != ArgumentType.KEYWORD_CALL) {
            return;
        }
        String linkString = argument.getUnescapedValue();
        IRegion linkRegion = new Region(argument.getArgCharPos(), argument.getValue().length());
        DefinitionFinder.acceptMatches(file, LineType.KEYWORD_TABLE_KEYWORD_BEGIN, new KeywordMatchVisitor(linkString, linkRegion, file, links));
        if (links.isEmpty()) {
            // try without possible BDD prefix
            String alternateValue = argument.getAlternateValue();
            if (alternateValue != null) {
                int origLength = linkString.length();
                linkString = RFTArgumentUtils.unescapeArgument(alternateValue, 0, alternateValue.length());
                int lengthDiff = origLength - linkString.length();
                linkRegion = new Region(argument.getArgCharPos() + lengthDiff, argument.getValue().length() - lengthDiff);
                DefinitionFinder.acceptMatches(file, LineType.KEYWORD_TABLE_KEYWORD_BEGIN, new KeywordMatchVisitor(linkString, linkRegion, file, links));
            }
        }
    }
}
