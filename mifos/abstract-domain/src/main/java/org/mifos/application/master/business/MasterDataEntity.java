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

package org.mifos.application.master.business;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.mifos.framework.business.AbstractEntity;

/**
 * Subclasses of this class provide access to the database tables which
 * correspond to enum-like classes.
 *
 * The replacement for a subclass of this class generally will be an enum. We
 * generally expect to move looking up messages from the database for
 * localization (language and MFI) to MessageLookup.
 */
public abstract class MasterDataEntity extends AbstractEntity {
    // values which override localized values are stored with locale =1
    public static Short CUSTOMIZATION_LOCALE_ID = (short) 1;

    public abstract Short getId();

    public abstract Set<LookUpValueLocaleEntity> getNames();

    public abstract String getName();

    public abstract void setLocaleId(Short locale);

    public abstract LookUpValueEntity getLookUpValue();

    public void update(String newValue) {
        Set<LookUpValueLocaleEntity> lookUpValueLocales = getLookUpValue().getLookUpValueLocales();
        if ((lookUpValueLocales != null) && StringUtils.isNotBlank(newValue)) {
            for (LookUpValueLocaleEntity entity : lookUpValueLocales) {
                if (entity.getLookUpId().equals(getLookUpValue().getLookUpId())
                        && (entity.getLookUpValue() == null || !entity.getLookUpValue().equals(newValue))) {
                    entity.setLookUpValue(newValue);
                    break;
                }
            }
        }

    }
}
