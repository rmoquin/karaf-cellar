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
package org.apache.karaf.cellar.config;

import org.apache.karaf.cellar.core.CellarSupport;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Generic configuration support.
 */
public class ConfigurationSupport extends CellarSupport {

    private static String[] EXCLUDED_PROPERTIES = {"service.factoryPid", "felix.fileinstall.filename", "felix.fileinstall.dir", "felix.fileinstall.tmpdir", "org.ops4j.pax.url.mvn.defaultRepositories"};

    private static final String FELIX_FILEINSTALL_FILENAME = "felix.fileinstall.filename";

    protected File storage;

    /**
     * Read a {@code Dictionary} and create a corresponding {@code Properties}.
     *
     * @param dictionary the source dictionary.
     * @return the corresponding properties.
     */
    public Properties dictionaryToProperties(Dictionary dictionary) {
        Properties properties = new Properties();
        //Dictionary seems to indicate that keys won't return null;
        if (dictionary != null) {

            Enumeration keys = dictionary.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                //Dictionary values can't be null
                Object value = dictionary.get(key);
                properties.put(key, value);
            }
        }
        return properties;
    }

    /**
     * Returns true if dictionaries are equal.
     *
     * @param source the source dictionary.
     * @param target the target dictionary.
     * @return true if the two dictionaries are equal, false else.
     */
    protected boolean equals(Dictionary source, Dictionary target) {
        if (source == null && target == null)
            return true;

        if (source == null || target == null)
            return false;

        if (source.isEmpty() && target.isEmpty())
            return true;

        //Might as well check this first before comparing items.
        if (source.size() != target.size())
            return false;
        
        Enumeration sourceKeys = source.keys();
        while (sourceKeys.hasMoreElements()) {
            Object key = sourceKeys.nextElement();
            
            //No need to convert this to a string simply to compare the objects.
            Object sourceValue = source.get(key);
            Object targetValue = target.get(key);
//          The Dictionary object says that neither keys or values can be null, so don't bother with a check
            if (!sourceValue.equals(targetValue))
                return false;
        }

        return true;
    }

    /**
     * Filter a dictionary, and populate a target dictionary.
     *
     * @param dictionary the source dictionary.
     * @return the filtered dictionary
     */
    public Dictionary filter(Dictionary dictionary) {
        Dictionary result = new Properties();
        if (dictionary != null) {
            Enumeration sourceKeys = dictionary.keys();
            while (sourceKeys.hasMoreElements()) {
                String key = (String) sourceKeys.nextElement();
                if (!isExcludedProperty(key)) {
                    //No point in converting to string here.
                    Object value = dictionary.get(key);
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Check if a property is in the default excluded list.
     *
     * @param propertyName the property name to check.
     * @return true is the property is excluded, false else.
     */
    public boolean isExcludedProperty(String propertyName) {
        for (int i = 0; i < EXCLUDED_PROPERTIES.length; i++) {
            if (EXCLUDED_PROPERTIES[i].equals(propertyName))
                return true;
        }
        return false;
    }

    /**
     * Persist a configuration to a storage.
     *
     * @param admin the configuration admin service.
     * @param pid the configuration PID to store.
     * @param props the properties to store, linked to the configuration PID.
     * @throws Exception in case of store failure.
     */
    protected void persistConfiguration(ConfigurationAdmin admin, String pid, Dictionary props) {
        try {
            if (pid.matches(".*-.*-.*-.*-.*")) {
                // it's UUID
                return;
            }
            File storageFile = new File(storage, pid + ".cfg");
            Configuration cfg = admin.getConfiguration(pid, null);
            if (cfg != null && cfg.getProperties() != null) {
                Object val = cfg.getProperties().get(FELIX_FILEINSTALL_FILENAME);
                try {
                    if (val instanceof URL) {
                        storageFile = new File(((URL) val).toURI());
                    }
                    if (val instanceof URI) {
                        storageFile = new File((URI) val);
                    }
                    if (val instanceof String) {
                        storageFile = new File(new URL((String) val).toURI());
                    }
                } catch (Exception e) {
                    //No need to cast if you use a different constructor.
                    throw new IOException(e.getMessage(), e);
                }
            }
            //The super class specified that this properties object uses Strings for keys and values...
            org.apache.felix.utils.properties.Properties p = new org.apache.felix.utils.properties.Properties(storageFile);
            
            //but do this first, otherwise you are going to double check values added from
            //the props arguement that was passed in and you'll already know they pass that test..
            // remove "removed" properties from the file
            List<String> propertiesToRemove = new ArrayList<String>();
            Set<String> set = p.keySet();
            for (String key : set) {
                //The key can't be null so remove that check.
                if (!org.osgi.framework.Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FELIX_FILEINSTALL_FILENAME.equals(key)) {
                    propertiesToRemove.add(key);
                }
            }
            for (String key : propertiesToRemove) {
                p.remove(key);
            }
            
            for (Enumeration<String> keys = props.keys(); keys.hasMoreElements(); ) {
                String key = keys.nextElement();
                if (!org.osgi.framework.Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FELIX_FILEINSTALL_FILENAME.equals(key)) {
                    p.put(key, (String) props.get(key));
                }
            }
            // save the cfg file
            storage.mkdirs();
            p.save();
        } catch (Exception e) {
            // nothing to do
        }
    }

    /**
     * Delete the storage of a configuration.
     *
     * @param pid the configuration PID to delete.
     */
    protected void deleteStorage(String pid) {
        File cfgFile = new File(storage, pid + ".cfg");
        cfgFile.delete();
    }

    public File getStorage() {
        return storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

}
