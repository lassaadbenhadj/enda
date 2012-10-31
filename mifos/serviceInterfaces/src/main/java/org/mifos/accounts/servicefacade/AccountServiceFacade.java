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

package org.mifos.accounts.servicefacade;

import org.mifos.dto.domain.AccountPaymentParametersDto;
import org.mifos.dto.domain.ApplicableCharge;
import org.mifos.dto.domain.UserReferenceDto;
import org.mifos.dto.screen.AccountTypeCustomerLevelDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.List;

/**
 * Interface for presentation layer to access accounts
 *
 */
public interface AccountServiceFacade {

    @PreAuthorize("isFullyAuthenticated()")
    List<ApplicableCharge> getApplicableFees(Integer accountId);

    @PreAuthorize("isFullyAuthenticated()")
    void applyCharge(Integer accountId, Short feeId, Double chargeAmount);

    @PreAuthorize("isFullyAuthenticated()")
    AccountTypeCustomerLevelDto getAccountTypeCustomerLevelDto(Integer accountId);

    @PreAuthorize("isFullyAuthenticated()")
    AccountPaymentDto getAccountPaymentInformation(Integer accountId, String paymentType, Short localeId, UserReferenceDto userReferenceDto, Date paymentDate);

    @PreAuthorize("isFullyAuthenticated()")
    boolean isPaymentPermitted(Integer accountId);

    @PreAuthorize("isFullyAuthenticated()")
    void makePayment(AccountPaymentParametersDto accountPaymentParametersDto);

    @PreAuthorize("isFullyAuthenticated()")
    void applyAdjustment(String globalAccountNum, String adjustmentNote, Short personnelId);
}