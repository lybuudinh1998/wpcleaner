/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2023  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */


package org.wikipediacleaner.api.data;

import java.util.List;

import org.wikipediacleaner.api.configuration.WPCConfiguration;
import org.wikipediacleaner.api.configuration.WPCConfigurationStringList;
import org.wikipediacleaner.api.data.PageElementTag.Parameter;
import org.wikipediacleaner.api.data.analysis.PageAnalysis;
import org.wikipediacleaner.api.data.contents.tag.WikiTagType;

public final class PageElementTagRef {

  private PageElementTagRef() {
    // Utility class
  }

  public static String getGroup(PageElementTag tag, PageAnalysis analysis) {

    // Check for a group parameter in the tag
    Parameter group = tag.getParameter("group");
    if (group != null) {
      return formatGroupName(group.getValue());
    }

    // Check for a group parameter in the references tag
    PageElementTag references = analysis.getSurroundingTag(
        WikiTagType.REFERENCES, tag.getBeginIndex());
    if (references != null) {
      group = references.getParameter("group");
      if (group != null) {
        return formatGroupName(group.getValue());
      }
      return null;
    }

    // Check for a group parameter in the references templates
    WPCConfiguration config = analysis.getWPCConfiguration();
    List<String[]> templates = config.getStringArrayList(WPCConfigurationStringList.REFERENCES_TEMPLATES);
    if (templates != null) {
      PageElementTemplate template = analysis.isInTemplate(tag.getBeginIndex());
      if (template != null) {
        for (String[] elements : templates) {
          if ((elements.length > 1) &&
              (Page.areSameTitle(template.getTemplateName(), elements[0]))) {
            String[] argNames = elements[1].split(",");
            for (String argName : argNames) {
              String tmp = template.getParameterValue(argName);
              if (tmp != null) {
                if ((tmp.length() > 2) &&
                    (tmp.charAt(0) == '"') &&
                    (tmp.charAt(tmp.length() - 1) == '"')) {
                  tmp = tmp.substring(1, tmp.length() - 2);
                }
                return formatGroupName(tmp);
              }
            }
          }
        }
      }
    }

    return null;
  }

  private static String formatGroupName(final String groupName) {
    if ((groupName == null) || (groupName.trim().length() == 0)) {
      return null;
    }
    return groupName.trim();
  }
}
