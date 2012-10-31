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

import org.mifos.framework.business.AbstractEntity;

public class LookUpLabelEntity extends AbstractEntity {

    private String labelName;

    private Short LookUpLabelId;

    private Short localeId;

    private SupportedLocalesEntity locale;

    private LookUpEntity lookUpEntity;

    public String getLabelKey() {
        return lookUpEntity.getEntityType();
    }

    public String getLabelText() {
        if (labelName != null && labelName.length() > 0) {
            return labelName;
        }

        // if we don't find a label here, then it means that it has not been
        // customized and
        // we should return the default label from the properties file

        // note: instead of message lookup here, we use it further out and use labelkey
        return "";
    }

    /*
     * This method is only for use by Hibernate to persist this class. To get
     * the label text back use {@link getLabelText()}
     */
    protected String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public SupportedLocalesEntity getLocale() {
        return locale;
    }

    public void setLocale(SupportedLocalesEntity locale) {
        this.locale = locale;
    }

    public void setLocaleId(Short localeId) {
        this.localeId = localeId;
    }

    public Short getLocaleId() {
        return localeId;
    }

    public Short getLookUpLabelId() {
        return LookUpLabelId;
    }

    public void setLookUpLabelId(Short lookUpLabelId) {
        LookUpLabelId = lookUpLabelId;
    }

    public LookUpEntity getLookUpEntity() {
        return lookUpEntity;
    }

    public void setLookUpEntity(LookUpEntity lookUpEntity) {
        this.lookUpEntity = lookUpEntity;
    }

}
