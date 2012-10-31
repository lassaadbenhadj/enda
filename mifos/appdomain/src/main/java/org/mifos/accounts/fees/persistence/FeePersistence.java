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

package org.mifos.accounts.fees.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.mifos.accounts.fees.business.AmountFeeBO;
import org.mifos.accounts.fees.business.FeeBO;
import org.mifos.accounts.fees.business.RateFeeBO;
import org.mifos.accounts.fees.util.helpers.FeeCategory;
import org.mifos.accounts.fees.util.helpers.FeeStatus;
import org.mifos.accounts.fees.util.helpers.RateAmountFlag;
import org.mifos.application.NamedQueryConstants;
import org.mifos.core.MifosRuntimeException;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.persistence.LegacyGenericDao;

/**
 * @deprecated move to {@link FeeDao} and replace all usage of this methods with calls to {@link FeeDao}.
 */
@Deprecated
public class FeePersistence extends LegacyGenericDao {

    /**
     * use {@link FeeDao#findById(Short)}
     */
    @Deprecated
    public FeeBO getFee(Short feeId) {
        Session session = StaticHibernateUtil.getSessionTL();
        return (FeeBO) session.get(FeeBO.class, feeId);
    }

    public FeeBO getFee(Short feeId, RateAmountFlag rateflag) throws PersistenceException {

        if (rateflag.equals(RateAmountFlag.AMOUNT)) {
            return getPersistentObject(AmountFeeBO.class, feeId);
        }

        return getPersistentObject(RateFeeBO.class, feeId);
    }

    @SuppressWarnings("unchecked")
    public List<Short> getUpdatedFeesForCustomer() throws PersistenceException {
        return executeNamedQuery("retrieveUpdatedFeesApplicableToCustomers", null);
    }

    @SuppressWarnings("unchecked")
    public List<FeeBO> getAllAppllicableFeeForLoanCreation() throws PersistenceException {
        HashMap<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("active", FeeStatus.ACTIVE.getValue());
        queryParameters.put("category", FeeCategory.LOAN.getValue());
        return executeNamedQuery(NamedQueryConstants.GET_ALL_APPLICABLE_FEE_FOR_LOAN_CREATION, queryParameters);
    }

    public RateFeeBO getRateFee(Short feeId) {
        Session session = StaticHibernateUtil.getSessionTL();
        return (RateFeeBO) session.get(RateFeeBO.class, feeId);
    }

    public FeeBO findFeeById(final Short feeId) {
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put("feeId", feeId);

        try {
            return (FeeBO) execUniqueResultNamedQuery("findFeeById", queryParameters);
        } catch (PersistenceException e) {
            throw new MifosRuntimeException(e);
        }
    }
}
