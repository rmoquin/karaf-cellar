/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.core;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cellar generic support. This class provides a set of util methods used by other classes.
 */
public class CellarSupport {

    /**
     * Check if a resource is allowed for a type of cluster event.
     *
     * @param url the resource name.
     * @param whiteList the whitelisted items for a particular event.
     * @param blackList the blacklisted items for a particular event.
     * @return
     */
    public boolean isAllowed(String event, Set<String> whiteList, Set<String> blackList) {
        boolean result = true;

        // if no white listed items we assume all are accepted.
        if (whiteList != null && !whiteList.isEmpty()) {
            result = false;
            for (String whiteListItem : whiteList) {
                if (wildCardMatch(event, whiteListItem)) {
                    result = true;
                }
            }
        }

        // if any blackList item matched, then false is returned.
        if (blackList != null && !blackList.isEmpty()) {
            for (String blackListItem : blackList) {
                if (wildCardMatch(event, blackListItem)) {
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Check if a string match a regex.
     *
     * @param item the string to check.
     * @param pattern the regex pattern.
     * @return true if the item string matches the pattern, false else.
     */
    protected boolean wildCardMatch(String item, String pattern) {
        // update the pattern to have a valid regex pattern
        pattern = pattern.replace("*", ".*");
        // use the regex
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(item);
        return m.matches();
    }
}
