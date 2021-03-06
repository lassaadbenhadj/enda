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

package org.mifos.framework.struts.tags;

import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.struts.taglib.TagUtils;
import org.mifos.application.master.business.CustomValueListElementDto;

/**
 * This class renders a listbox
 */

public class MifosValueList extends BodyTagSupport {

    private static final Logger logger = LoggerFactory.getLogger(MifosValueList.class);
    /**
     * Name of the bean action form which contains the listbox. This name is
     * defined in the struts-config.xml file. For example, the name of the bean
     * for Define Lookup Option form is lookupoptionsactionform as defined in
     * the struts-config.xml file <form-bean name="lookupoptionsactionform"
     * type=
     * "org.mifos.config.struts.actionform.LookupOptionsActionForm"
     * ></form-bean>
     */
    private String name;

    /* property is the name of the listbox */
    private String property;

    /**
     * property2 is the name of the string array which is used to populate the
     * list. This name is the member of the bean form so the bean form needs to
     * have the get/set functions for this member. The first letter of this
     * property has to be in upper case because in this class this function
     * obj.getClass().getDeclaredMethod("get" + getProperty(),(Class[]) null);
     * is used.
     */
    private String property2;

    static final long serialVersionUID = 851220505975730594L;
    /* listbox size */
    private String size;
    /* listbox height and width */
    private String style;

    /**
     * used to get userContext object
     */
    // TODO: string right now, may be object later
    private String userContext;

    /**
     * Return the property that represents the input list
     */
    public String getProperty() {
        return property;
    }

    /**
     * Set the property that represents the input list
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Function get the name of the bean
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the bean
     */
    public void setName(String name) {
        this.name = name;
    }

    public MifosValueList() {
        super();
    }

    public String getUserContext() {
        return userContext;
    }

    public void setUserContext(String usercontext) {
        this.userContext = usercontext;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getProperty2() {
        return property2;
    }

    public void setProperty2(String property2) {
        this.property2 = property2;
    }

    /**
     * variable to hold the getlist method
     */

    private Method getList = null;

    /**
     * Function to render the tag in jsp
     *
     * @throws JspException
     */
    @Override
    public int doEndTag() throws JspException {

        StringBuffer results = new StringBuffer();
        List<CustomValueListElementDto> inputList = null;
        Object obj = null;
        try {
            logger.debug("Inside doEndTag of MifosValueList Tag");
            obj = pageContext.findAttribute(getName());
            if (null == obj) {
                logger.debug(
                        "Can't get the bean form from the bean name");
                throw new Exception(
                        "Can't get the bean form from the bean name. Please check the bean name defined in the name attribute");
            }
            logger.debug("object is " + obj);

            if (null == getUserContext()) {

                logger.debug("userContext is null");

                getList = obj.getClass().getDeclaredMethod("get" + getProperty2(), (Class[]) null);
                logger.debug("method called is " + getList);

                inputList = (List<CustomValueListElementDto>) getList.invoke(obj, (Object[]) null);

                logger.debug("List got is " + inputList);
            } else {
                logger.debug("userContext is not null");

                getList = obj.getClass().getDeclaredMethod("get" + getProperty2(), new Class[] { Object.class });

                logger.debug("method called is " + getList);

                inputList = (List<CustomValueListElementDto>) getList.invoke(obj, new Object[] { new Object() });

                logger.debug("List got is " + inputList);
            }

        } catch (Exception e) {
            throw new JspException(e.getMessage());
        }

        results = render(inputList);
        TagUtils.getInstance().write(pageContext, results.toString());
        return super.doEndTag();
    }

    private void prepareAttribute(StringBuffer handlers, String name, Object value) {
        if (value != null) {
            handlers.append(" ");
            handlers.append(name);
            handlers.append("=\"");
            handlers.append(value);
            handlers.append("\"");
        }
    }

    public StringBuffer render(List<CustomValueListElementDto> inputList) {
        super.toString();
        StringBuffer results = new StringBuffer();
        results.append("<SELECT ");
        prepareAttribute(results, "name", getProperty());
        prepareAttribute(results, "style", getStyle());
        prepareAttribute(results, "size", getSize());

        results.append(">");
        if (inputList != null) {
            for (CustomValueListElementDto element : inputList) {
                results.append("<OPTION value=\"" + mapOriginalCustomValueListElementToString(element) + "\">"
                        + element.getLookUpValue() + "</OPTION>");
            }
        }
        results.append("</SELECT> ");

        return results;

    }

    /**
     * Map a CustomValueListElementDto to a String of the form:
     * "CustomValueListId;CustomValueListValue;original"
     *
     */
    private static String mapCustomValueListElementToString(CustomValueListElementDto element) {
        Integer id = element.getLookUpId();
        if (id == null) {
            id = 0;
        }
        return "" + id + ";" + element.getLookUpValue();
    }

    public static String mapAddedCustomValueListElementToString(CustomValueListElementDto element) {
        return mapCustomValueListElementToString(element);
    }

    public static String mapUpdatedCustomValueListElementToString(CustomValueListElementDto element) {
        return mapCustomValueListElementToString(element);
    }

    public static String mapOriginalCustomValueListElementToString(CustomValueListElementDto element) {
        return mapCustomValueListElementToString(element);
    }

}
