/*
 *  WikipediaCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2008  Nicolas Vervelle
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipediacleaner.api.check.algorithm;

import java.util.Collection;
import java.util.List;

import org.wikipediacleaner.api.check.CheckErrorResult;
import org.wikipediacleaner.api.data.MagicWord;
import org.wikipediacleaner.api.data.Page;
import org.wikipediacleaner.api.data.PageElementComment;
import org.wikipediacleaner.i18n.GT;


/**
 * Algorithm for analyzing error 88 of check wikipedia project.
 * Error 88: DEFAULTSORT with blank at first position
 */
public class CheckErrorAlgorithm088 extends CheckErrorAlgorithmBase {

  /**
   * Possible global fixes.
   */
  private final static String[] globalFixes = new String[] {
    GT._("Fix DEFAULTSORT"),
  };

  public CheckErrorAlgorithm088() {
    super("DEFAULTSORT with blank at first position");
  }

  /**
   * Analyze a page to check if errors are present.
   * 
   * @param page Page.
   * @param contents Page contents (may be different from page.getContents()).
   * @param comments Comments in the page contents.
   * @param errors Errors found in the page.
   * @return Flag indicating if the error was found.
   */
  public boolean analyze(
      Page page, String contents,
      Collection<PageElementComment> comments,
      Collection<CheckErrorResult> errors) {
    if ((page == null) || (contents == null)) {
      return false;
    }

    // Analyzing the text from the beginning
    boolean result = false;
    int startIndex = 0;
    while (startIndex < contents.length()) {
      // Update position of next {{
      int beginIndex = contents.indexOf("{{", startIndex);

      if (beginIndex < 0) {
        // No more {{
        startIndex = contents.length();
      } else {
        int currentPos = beginIndex + 2;

        // Update position of next }}
        int endIndex = contents.indexOf("}}", currentPos);

        if (endIndex < 0) {
          startIndex = contents.length();
        } else {

          // Possible whitespaces
          while ((currentPos < endIndex) && Character.isWhitespace(contents.charAt(currentPos))) {
            currentPos++;
          }

          // Check that link is DEFAULTSORT
          String defaultSort = null;
          if (currentPos < endIndex) {
            MagicWord magicDefaultsort = page.getWikipedia().getMagicWord(MagicWord.DEFAULT_SORT);
            List<String> aliases = magicDefaultsort.getAliases();
            for (int i = 0; (i < aliases.size()) && (defaultSort == null); i++) {
              if (contents.startsWith(aliases.get(i), currentPos)) {
                currentPos += aliases.get(i).length();
                defaultSort = aliases.get(i);
              }
            }
          }
          // DEFAULTSORT found
          if ((currentPos < endIndex) && (defaultSort != null)) {
            int beginLink = currentPos;
            while ((currentPos < endIndex) && (contents.charAt(currentPos) == ' ')) {
              currentPos++;
            }
            if (currentPos > beginLink) {
              if (errors == null) {
                return true;
              }
              result = true;
              CheckErrorResult errorResult = createCheckErrorResult(page, beginIndex, endIndex + 2);
              errorResult.addReplacement(
                  "{{" + defaultSort + contents.substring(currentPos, endIndex + 2));
              errors.add(errorResult);
            }
          }
          startIndex = endIndex + 2;
        }
      }
    }
    return result;
  }

  /**
   * @return List of possible global fixes.
   */
  @Override
  public String[] getGlobalFixes() {
    return globalFixes;
  }

  /**
   * Fix all the errors in the page.
   * 
   * @param fixName Fix name (extracted from getGlobalFixes()).
   * @param page Page.
   * @param contents Page contents (may be different from page.getContents()).
   * @return Page contents after fix.
   */
  @Override
  public String fix(String fixName, Page page, String contents) {
    return fixUsingFirstReplacement(fixName, page, contents);
  }
}
