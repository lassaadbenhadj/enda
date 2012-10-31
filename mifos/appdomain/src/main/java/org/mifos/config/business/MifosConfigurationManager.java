/*
 * Copyright (c) 2005-2011 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.config.business;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.util.ConfigurationLocator;
import org.mifos.service.test.TestMode;
import org.springframework.core.io.ClassPathResource;

/**
 * This is a quick initial sketch of a class for managing configuration values
 * that come from various sources. The intent is to use file based configuration
 * values for some configuration data and database based configuration for other
 * configuration data.
 * <p>
 * The general idea is that configuration that does not change often (or should
 * not be changed often) would go into configuration files while more frequently
 * changed configuration would be stored in the database and exposed via the UI.
 * In particular, configuration values that should be set once at install time
 * or not be changed after being set are likely to go into configuration files.
 * <p>
 * This class is currently under active development, so it is likely to be
 * changed significantly as iterative development proceeds.
 */
public class MifosConfigurationManager implements Configuration {

    private static final Logger logger = Logger.getLogger(MifosConfigurationManager.class.getName());

    /**
     * Filename where default application-wide configuration values are stored.
     * This file should never be hand-edited, edit values in the custom config
     * file instead.
     *
     * @see #CUSTOM_CONFIG_PROPS_FILENAME
     */
    public static final String DEFAULT_CONFIG_PROPS_FILENAME = "applicationConfiguration.default.properties";

    /**
     * Filename where custom overrides for application-wide configuration values
     * are stored. Keys in this file must exist in the default config file. This
     * file may be hand-edited.
     *
     * @see #DEFAULT_CONFIG_PROPS_FILENAME
     */
    public static final String CUSTOM_CONFIG_PROPS_FILENAME = "applicationConfiguration.custom.properties";

    /** Custom application-wide configuration file for acceptance testing only. */
    public static final String ACCEPTANCE_CONFIG_PROPS_FILENAME = "applicationConfiguration.acceptance.properties";

    private static MifosConfigurationManager configurationManagerInstance = new MifosConfigurationManager();

    private Configuration configuration;

    public static final MifosConfigurationManager getInstance() {
        if(configurationManagerInstance == null || configurationManagerInstance.isEmpty()) {
            configurationManagerInstance = new MifosConfigurationManager();
        }
        return configurationManagerInstance;
    }

    private MifosConfigurationManager() {

        String configFilePath = "org/mifos/config/resources/applicationConfiguration.default.properties";
        ClassPathResource configfile = new ClassPathResource(configFilePath);
        logger.info("Checking existance of: " + configFilePath);

        Properties props = new Properties();
        if (configfile.exists()) {
            logger.info(configfile.getFilename() + " exists on classpath");
            InputStream in = MifosConfigurationManager.class.getClassLoader().getResourceAsStream(configFilePath);
            if (in == null) {
                //File not found! (Manage the problem)
            }
            try {
                props.load(in);
                logger.info(props.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ConfigurationLocator configurationLocator = new ConfigurationLocator();
//
//        try {
//            File defaults = configurationLocator.getFile(DEFAULT_CONFIG_PROPS_FILENAME);
//            props.load(new BufferedInputStream(new FileInputStream(defaults)));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        // FIXME - see if we can remove use of standard testing service from here?
//        TestMode currentTestMode = new StandardTestingService().getTestMode();
        TestMode currentTestMode = TestMode.MAIN;
        File customConfigFile = null;

        try {
            if (TestMode.MAIN == currentTestMode) {
                customConfigFile = configurationLocator.getFile(CUSTOM_CONFIG_PROPS_FILENAME);
                props.load(new BufferedInputStream(new FileInputStream(customConfigFile)));
            } else if (TestMode.ACCEPTANCE == currentTestMode) {
                customConfigFile = configurationLocator.getFile(ACCEPTANCE_CONFIG_PROPS_FILENAME);
                props.load(new BufferedInputStream(new FileInputStream(customConfigFile)));
            }
        } catch (FileNotFoundException e) {
            /*
             * An FileNotFoundException will be thrown if a file is not found by
             * getFile(); for normal runtime and acceptance testing modes, the
             * custom config file is optional, hence, ignore the exception.
             */
        } catch (IOException e) {
            /*
             * An IOException is thrown by ClassPathResource if the file is not
             * found. Integration tests require a (dummy) custom config file and
             * we should fail if the file is missing.
             */
            throw new SystemException(e);
        }

        configuration = ConfigurationConverter.getConfiguration(props);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        return configuration.getShort(key, defaultValue);
    }

    @Override
    public short getShort(String key, short defaultValue) {
        return configuration.getShort(key, defaultValue);
    }

    @Override
    public short getShort(String key) {
        return configuration.getShort(key);
    }

    @Override
    public float getFloat(String key) {
        return configuration.getFloat(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return configuration.getInt(key, defaultValue);
    }

    @Override
    public int getInt(String key) {
        return configuration.getInt(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return configuration.getDouble(key, defaultValue);
    }

    @Override
    public double getDouble(String key) {
        return configuration.getDouble(key);
    }

    @Override
    public boolean containsKey(String key) {
        return configuration.containsKey(key);
    }

    @Override
    public void addProperty(String propertyName, Object propertyValue) {
        configuration.addProperty(propertyName, propertyValue);
    }

    @Override
    public void clearProperty(String propertyName) {
        configuration.clearProperty(propertyName);
    }

    @Override
    public void clear() {
        configuration.clear();
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return configuration.getBigDecimal(key);
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return configuration.getBigDecimal(key, defaultValue);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return configuration.getBigInteger(key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return configuration.getBigInteger(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key) {
        return configuration.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return configuration.getBoolean(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return configuration.getBoolean(key, defaultValue);
    }

    @Override
    public byte getByte(String key) {
        return configuration.getByte(key);
    }

    @Override
    public byte getByte(String key, byte defaultValue) {
        return configuration.getByte(key, defaultValue);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return configuration.getByte(key, defaultValue);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        return configuration.getDouble(key, defaultValue);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return configuration.getFloat(key, defaultValue);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        return configuration.getFloat(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return configuration.getInteger(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator getKeys() {
        return configuration.getKeys();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator getKeys(String key) {
        return configuration.getKeys(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List getList(String key) {
        return configuration.getList(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List getList(String key, List defaultValue) {
        return configuration.getList(key, defaultValue);
    }

    @Override
    public long getLong(String key) {
        return configuration.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return configuration.getLong(key, defaultValue);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        return configuration.getLong(key, defaultValue);
    }

    @Override
    public Properties getProperties(String key) {
        return configuration.getProperties(key);
    }

    @Override
    public Object getProperty(String key) {
        return configuration.getProperty(key);
    }

    @Override
    public String getString(String key) {
        return configuration.getString(key);
    }

    @Override
    public String[] getStringArray(String key) {
        return configuration.getStringArray(key);
    }

    @Override
    public boolean isEmpty() {
        return configuration.isEmpty();
    }

    @Override
    public void setProperty(String propertyName, Object propertyValue) {
        configuration.setProperty(propertyName, propertyValue);
    }

    // Return a decorator Configuration containing every key from the current
    // Configuration
    // that starts with the specified prefix
    @Override
    public Configuration subset(String prefix) {
        return configuration.subset(prefix);
    }

}
