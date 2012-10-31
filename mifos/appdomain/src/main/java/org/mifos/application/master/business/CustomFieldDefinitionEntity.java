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

import org.apache.commons.lang.StringUtils;
import org.mifos.application.master.MessageLookup;
import org.mifos.application.util.helpers.EntityType;
import org.mifos.application.util.helpers.YesNoFlag;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.framework.business.AbstractEntity;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.SearchUtils;
import org.mifos.security.activity.DynamicLookUpValueCreationTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Represents a named custom field of a given type {@link CustomFieldType}
 * defined for a particular entity {@link EntityType}.
 */

public class CustomFieldDefinitionEntity extends AbstractEntity {

    private static final Short DEFAULT_LOCALE_ID = 1;

    private Short fieldId;

    /*
     * The name of the custom field
     */
    private final LookUpEntity lookUpEntity;

    private final Short levelId;

    /*
     * The data type {@link CustomFieldType}
     */
    private final Short fieldType;

    /*
     * see {@link EntityType}
     */
    private final Short entityType;

    private String defaultValue;

    private Short mandatoryFlag;

    /*
     * Adding a default constructor is hibernate's requirement and should not be
     * used to create a valid Object.
     */
    protected CustomFieldDefinitionEntity() {
        this.fieldId = null;
        this.lookUpEntity = null;
        this.levelId = null;
        this.fieldType = null;
        this.entityType = null;
        this.defaultValue = null;
        this.mandatoryFlag = null;
    }

    public CustomFieldDefinitionEntity(LookUpEntity name, Short fieldIndex, CustomFieldType fieldType,
            EntityType entityType, String defaultValue, YesNoFlag mandatory) {
        this.fieldId = null; // this should be assigned when persisted to the
                             // database
        this.lookUpEntity = name;
        this.levelId = fieldIndex;
        this.fieldType = fieldType.getValue();
        this.entityType = entityType.getValue();
        this.defaultValue = defaultValue;
        this.mandatoryFlag = mandatory.getValue();

    }

    /*
     * Create all custom fields with values stored in the default (1) locale
     */
    public CustomFieldDefinitionEntity(String label, Short levelId, CustomFieldType fieldType, EntityType entityType,
            String defaultValue, YesNoFlag mandatory) {
        this(label, levelId, fieldType, entityType, defaultValue, mandatory, DEFAULT_LOCALE_ID);
    }

    /*
     * This constructor is used to create a custom field and after that it will
     * be saved to the database using addCustomField of
     * ApplicationConfigurationPersistence
     */
    private CustomFieldDefinitionEntity(String label, Short levelId, CustomFieldType fieldType, EntityType entityType,
            String defaultValue, YesNoFlag mandatory, Short localeId) {

        LookUpEntity lookupEntity = new LookUpEntity();
        // add a timestamp so that we get a unique identifier
        // the label that someone enters can potentially collide with
        // the name of another unrelated entity, causing problems with
        // label lookup in the MifosConfiguration class
        String labelName = SearchUtils.generateLookupName(DynamicLookUpValueCreationTypes.CustomField.name(), label);
        lookupEntity.setEntityType(labelName);

        Set<LookUpLabelEntity> lookUpLabels = new HashSet<LookUpLabelEntity>();
        LookUpLabelEntity lookupLabel = new LookUpLabelEntity();
        lookupLabel.setLabelName(label);
        lookupLabel.setLookUpEntity(lookupEntity);
        lookupLabel.setLocaleId(localeId);
        lookUpLabels.add(lookupLabel);
        lookupEntity.setLookUpLabels(lookUpLabels);

        Set<LookUpValueEntity> lookUpValues = new HashSet<LookUpValueEntity>();
        LookUpValueEntity lookupValue = new LookUpValueEntity();
        lookupValue.setLookUpName(labelName);
        lookupValue.setLookUpEntity(lookupEntity);
        lookUpValues.add(lookupValue);
        lookupEntity.setLookUpValues(lookUpValues);
        this.lookUpEntity = lookupEntity;
        this.fieldType = fieldType.getValue();
        this.levelId = levelId;
        this.defaultValue = defaultValue;
        this.mandatoryFlag = mandatory.getValue();
        this.entityType = entityType.getValue();
        this.fieldId = null;

    }

    public Short getFieldId() {
        return fieldId;
    }

    public LookUpEntity getLookUpEntity() {
        return this.lookUpEntity;
    }

    public Short getLevelId() {
        return this.levelId;
    }

    public Short getFieldType() {
        return this.fieldType;
    }

    public CustomFieldType getFieldTypeAsEnum() {
        return CustomFieldType.fromInt(this.fieldType.intValue());
    }

    public Short getEntityType() {
        return this.entityType;
    }

    public String getEntityName() {
        return this.lookUpEntity.getEntityType();
    }

    @SuppressWarnings("unused")
    private Short getMandatoryFlag() {
        return this.mandatoryFlag;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMandatory() {
        return mandatoryFlag.equals(YesNoFlag.YES.getValue());
    }

    public void setMandatoryFlag(Short mandatoryFlag) {
        this.mandatoryFlag = mandatoryFlag;
    }

    @Override
    public boolean equals(Object obj) {
        CustomFieldDefinitionEntity customFieldDefinition = (CustomFieldDefinitionEntity) obj;
        if (this.entityType.equals(customFieldDefinition.getEntityType())
                && this.levelId.equals(customFieldDefinition.getLevelId())
                && this.lookUpEntity.equals(customFieldDefinition.getLookUpEntity())
                && this.fieldType.equals(customFieldDefinition.getFieldType())) {
            return true;
        }

        return false;
    }

    public static String getMandatoryStringValue(Locale locale, Short flag) {
        return MessageLookup.getInstance().lookup(YesNoFlag.fromInt(flag), locale);
    }

    public String getMandatoryStringValue(Locale locale) {
        return MessageLookup.getInstance().lookup(YesNoFlag.fromInt(mandatoryFlag), locale);
    }

    public void setLabel(String label) {
        for (LookUpLabelEntity entity : lookUpEntity.getLookUpLabels()) {
            if (entity.getLocaleId().equals(MasterDataEntity.CUSTOMIZATION_LOCALE_ID)) {
                entity.setLabelName(label);
                break;
            }
        }
    }

    public String getLabel() {
        return lookUpEntity.findLabel();
    }

    /*
     * This method is currently used as a property getter in various JSP pages.
     * Access to this value will need to be updated to support localization.
     */
    public String getMandatoryStringValue() {
        return getMandatoryStringValue(Locale.US).toLowerCase();
    }

    @Override
    public int hashCode() {
        return entityType.hashCode() * levelId.hashCode() * fieldType.hashCode();
    }

    public static List<CustomFieldDto> toDto(List<CustomFieldDefinitionEntity> customFieldDefinitions,
            Locale preferredUserLocale) {

        List<CustomFieldDto> customFieldDtos = new ArrayList<CustomFieldDto>();

        for (CustomFieldDefinitionEntity fieldDef : customFieldDefinitions) {

            CustomFieldDto fieldView;
            if (StringUtils.isNotBlank(fieldDef.getDefaultValue())
                    && fieldDef.getFieldType().equals(CustomFieldType.DATE.getValue())) {

                fieldView = new CustomFieldDto(fieldDef.getFieldId(), DateUtils.getUserLocaleDate(
                        preferredUserLocale, fieldDef.getDefaultValue()), fieldDef.getFieldType());
            } else {
                fieldView = new CustomFieldDto(fieldDef.getFieldId(), fieldDef.getDefaultValue(),
                        fieldDef.getFieldType());
            }

            fieldView.setMandatory(fieldDef.isMandatory());
            fieldView.setMandatoryString(fieldDef.getMandatoryStringValue());
            fieldView.setLookUpEntityType(fieldDef.getEntityName());
            fieldView.setLabel(fieldDef.getLabel());

            customFieldDtos.add(fieldView);
        }

        return customFieldDtos;
    }

    // To be used strictly from test code
    @Deprecated
    public void setFieldId(Short fieldId) {
        this.fieldId = fieldId;
    }
}