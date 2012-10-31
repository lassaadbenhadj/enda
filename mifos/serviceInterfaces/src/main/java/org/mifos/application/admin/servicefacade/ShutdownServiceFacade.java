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

package org.mifos.application.admin.servicefacade;

import org.springframework.security.access.prepost.PreAuthorize;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

public interface ShutdownServiceFacade {
    @PreAuthorize("isFullyAuthenticated()")
    List<LoggedUserDto> getLoggedUsers(HttpServletRequest request);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_OPEN_SHUTDOWN_PAGE')")
    String getStatus(HttpServletRequest request, Locale locale);

    boolean isShutdownInProgress(HttpServletRequest request);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_SHUT_DOWN_MIFOS')")
    void scheduleShutdown(HttpServletRequest request, long timeout);

    @PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CAN_SHUT_DOWN_MIFOS')")
    void cancelShutdown(HttpServletRequest request);
}