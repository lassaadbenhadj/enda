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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.mifos.application.master.MessageLookup;
import org.mifos.application.master.business.LookUpEntity;
import org.mifos.application.master.business.LookUpLabelEntity;
import org.mifos.application.master.business.LookUpValueEntity;
import org.mifos.application.master.business.MasterDataEntity;
import org.mifos.application.servicefacade.ApplicationContextProvider;
import org.mifos.config.LocalizedTextLookup;
import org.mifos.config.exceptions.ConfigurationException;
import org.mifos.config.persistence.ApplicationConfigurationDao;
import org.mifos.config.util.helpers.LabelKey;

/**
 * This class caches label text and lookUpValue text.
 *
 * Feb. 2008 - this class is slated to be removed. It is unclear if we need
 * caching and if we do, we should be able to use the Hibernate 2nd level cache
 * or Spring based caching support.
 */

public class MifosConfiguration {

    private Map<LabelKey, String> labelCache;

    private ApplicationConfigurationDao applicationConfigurationDao = ApplicationContextProvider.getBean(ApplicationConfigurationDao.class);

    private static final MifosConfiguration configuration = new MifosConfiguration();

    public static MifosConfiguration getInstance() {
        return configuration;
    }

    private MifosConfiguration() {
        labelCache = new ConcurrentHashMap<LabelKey, String>();
    }

    public void init() {
        initializeLabelCache();
    }

    public void updateLabelKey(String keyString, String newLabelValue, Short localeId) {
        synchronized (labelCache) {
            LabelKey key = new LabelKey(keyString, localeId);
            if (labelCache.containsKey(key)) {
                labelCache.remove(key);
                labelCache.put(key, newLabelValue);
            } else {
                labelCache.put(key, newLabelValue);
            }
        }
    }

    public void initializeLabelCache() {
        labelCache.clear();

        List<LookUpValueEntity> lookupValueEntities = applicationConfigurationDao.findLookupValues();
        for (LookUpValueEntity lookupValueEntity : lookupValueEntities) {
            String keyString = lookupValueEntity.getPropertiesKey();
            if (keyString == null) {
                keyString = " ";
            }

            String messageText = lookupValueEntity.getMessageText();
            if (StringUtils.isBlank(messageText)) {
                messageText = MessageLookup.getInstance().lookup(keyString);
            }

            labelCache.put(new LabelKey(keyString, MasterDataEntity.CUSTOMIZATION_LOCALE_ID), messageText);
        }

        List<LookUpEntity> entities = applicationConfigurationDao.findLookupEntities();
        for (LookUpEntity entity : entities) {
            Set<LookUpLabelEntity> labels = entity.getLookUpLabels();
            for (LookUpLabelEntity label : labels) {
                labelCache.put(new LabelKey(entity.getEntityType(), label.getLocaleId()), label.getLabelText());
            }
        }

    }

    public void updateKey(LocalizedTextLookup keyContainer, String newValue) {
        synchronized (labelCache) {
            LabelKey key = new LabelKey(keyContainer.getPropertiesKey(), MasterDataEntity.CUSTOMIZATION_LOCALE_ID);
            if (labelCache.containsKey(key)) {
                labelCache.remove(key);
                labelCache.put(key, newValue);
            } else {
                labelCache.put(key, newValue);
            }
        }
    }

    public void updateKey(String lookupValueKey, String newValue) {
        synchronized (labelCache) {
            LabelKey key = new LabelKey(lookupValueKey, MasterDataEntity.CUSTOMIZATION_LOCALE_ID);
            if (labelCache.containsKey(key)) {
                labelCache.remove(key);
                labelCache.put(key, newValue);
            } else {
                labelCache.put(key, newValue);
            }
        }
    }

    public void deleteKey(String lookupValueKey) {
        synchronized (labelCache) {
            LabelKey key = new LabelKey(lookupValueKey, MasterDataEntity.CUSTOMIZATION_LOCALE_ID);
            if (labelCache.containsKey(key)) {
                labelCache.remove(key);
            }
        }
    }

    public Map<LabelKey, String> getLabelCache() {
        return labelCache;
    }

    public String getLabelValue(String key, Short localeId) {
        return labelCache.get(new LabelKey(key, localeId));
    }

    public String getLabel(String key, Locale locale) throws ConfigurationException {
        // we only use localeId 1 to store labels since it is an override for
        // all locales
        return (key == null) ? null : getLabelValue(key, (short) 1);
    }

}
