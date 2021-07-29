/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.secret.mgt.core;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants;
import org.wso2.carbon.identity.secret.mgt.core.dao.SecretDAO;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementClientException;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;
import org.wso2.carbon.identity.secret.mgt.core.model.SecretManagerConfigurationHolder;
import org.wso2.carbon.identity.secret.mgt.core.model.Secrets;

import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_GET_DAO;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_INVALID_SECRET_ID;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_SECRET_ADD_REQUEST_INVALID;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_SECRET_ALREADY_EXISTS;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_SECRET_DELETE_REQUEST_REQUIRED;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_SECRET_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_SECRET_GET_REQUEST_INVALID;
import static org.wso2.carbon.identity.secret.mgt.core.constant.SecretConstants.ErrorMessages.ERROR_CODE_SECRET_REPLACE_REQUEST_INVALID;
import static org.wso2.carbon.identity.secret.mgt.core.util.SecretUtils.generateUniqueID;
import static org.wso2.carbon.identity.secret.mgt.core.util.SecretUtils.handleClientException;
import static org.wso2.carbon.identity.secret.mgt.core.util.SecretUtils.handleServerException;

/**
 * Secret Manager service implementation.
 */
public class SecretManagerImpl implements SecretManager {

    private static final Log log = LogFactory.getLog(SecretManagerImpl.class);
    private List<SecretDAO> secretDAOS;

    public SecretManagerImpl(SecretManagerConfigurationHolder secretManagerConfigurationHolder) {

        this.secretDAOS = secretManagerConfigurationHolder.getSecretDAOS();
    }

    @Override
    public Secret addSecret(Secret secret) throws SecretManagementException {

        validateSecretCreateRequest(secret);
        String secretId = generateUniqueID();
        if (log.isDebugEnabled()) {
            log.debug("Secret id generated: " + secretId);
        }
        secret.setSecretId(secretId);
        this.getSecretDAO().addSecret(secret);
        log.info("Secret: " + secret.getSecretName() + " added successfully");
        return secret;
    }

    @Override
    public Secret getSecret(String secretName) throws SecretManagementException {

        validateSecretRetrieveRequest(secretName);
        Secret secret = this.getSecretDAO().getSecretByName(getTenantId(), secretName);
        if (secret == null) {
            if (log.isDebugEnabled()) {
                log.debug("No secret found for the secretName: " + secretName);
            }
            throw handleClientException(ERROR_CODE_SECRET_DOES_NOT_EXISTS, secretName, null);
        }
        return secret;
    }

    @Override
    public Secrets getSecrets() throws SecretManagementException {

        List<Secret> secretList = this.getSecretDAO().getSecrets(getTenantId());
        if (secretList == null) {
            if (log.isDebugEnabled()) {
                log.debug("No secret found for the tenant: " + getTenantDomain());
            }
            throw handleClientException(
                    SecretConstants.ErrorMessages.ERROR_CODE_SECRETS_DOES_NOT_EXISTS, null);
        }
        return new Secrets(secretList);
    }

    @Override
    public Secret getSecretById(String secretId) throws SecretManagementException {

        if (StringUtils.isBlank(secretId)) {
            throw handleClientException(ERROR_CODE_INVALID_SECRET_ID, secretId);
        }
        Secret secret = this.getSecretDAO().getSecretById(getTenantId(), secretId);
        if (secret == null) {
            throw handleClientException(SecretConstants.ErrorMessages.ERROR_CODE_SECRET_ID_DOES_NOT_EXISTS, secretId);
        }
        return secret;
    }

    @Override
    public void deleteSecret(String secretName) throws SecretManagementException {

        validateSecretDeleteRequest(secretName);
        this.getSecretDAO().deleteSecretByName(getTenantId(), secretName);
        if (log.isDebugEnabled()) {
            log.debug("Secret: " + secretName + " is deleted successfully.");
        }
    }

    @Override
    public void deleteSecretById(String secretId) throws SecretManagementException {

        if (StringUtils.isBlank(secretId)) {
            throw handleClientException(ERROR_CODE_INVALID_SECRET_ID, secretId);
        }
        if (isSecretExistsById(secretId)) {
            this.getSecretDAO().deleteSecretById(getTenantId(), secretId);
            if (log.isDebugEnabled()) {
                log.debug("Secret id: " + secretId + " in tenant: " + getTenantDomain() + " deleted successfully.");
            }
        } else {
            throw handleClientException(SecretConstants.ErrorMessages.ERROR_CODE_SECRET_ID_DOES_NOT_EXISTS, secretId);
        }
    }

    @Override
    public Secret replaceSecret(Secret secret) throws SecretManagementException {

        validateSecretReplaceRequest(secret);
        String secretId = generateSecretId(secret.getSecretName());
        secret.setSecretId(secretId);
        this.getSecretDAO().replaceSecret(secret);
        log.info(secret.getSecretName() + " secret created successfully.");
        return secret;
    }

    /**
     * Validate that secret name is non-empty.
     *
     * @param secretName The secret name.
     * @throws SecretManagementException If secret validation fails.
     */
    private void validateSecretRetrieveRequest(String secretName) throws SecretManagementException {

        if (StringUtils.isEmpty(secretName)) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid secret identifier with secretName: " + secretName + ".");
            }
            throw handleClientException(ERROR_CODE_SECRET_GET_REQUEST_INVALID, null);
        }
    }

    /**
     * Validate that secret name is non-empty.
     * Set tenant domain if they are not set to the secret object.
     *
     * @param secretName The secret name.
     * @throws SecretManagementException If secret validation fails.
     */
    private void validateSecretDeleteRequest(String secretName)
            throws SecretManagementException {

        if (StringUtils.isEmpty(secretName)) {
            if (log.isDebugEnabled()) {
                log.debug("Error identifying the secret with secret name: " + secretName + ".");
            }
            throw handleClientException(ERROR_CODE_SECRET_DELETE_REQUEST_REQUIRED, null);
        }

        if (!isSecretExist(secretName)) {
            if (log.isDebugEnabled()) {
                log.debug("A secret with the name: " + secretName + " does not exists.");
            }
            throw handleClientException(ERROR_CODE_SECRET_DOES_NOT_EXISTS, secretName);
        }
    }

    /**
     * Validate that secret name and value are non-empty.
     * Set tenant domain if they are not set to the secret object.
     *
     * @param secret The secret to be added.
     * @throws SecretManagementException If secret validation fails.
     */
    private void validateSecretCreateRequest(Secret secret) throws SecretManagementException {

        if (StringUtils.isEmpty(secret.getSecretName()) || StringUtils.isEmpty(secret.getValue())) {
            throw handleClientException(ERROR_CODE_SECRET_ADD_REQUEST_INVALID, null);
        }
        if (isSecretExist(secret.getSecretName())) {
            throw handleClientException(ERROR_CODE_SECRET_ALREADY_EXISTS, secret.getSecretName());
        }
        if (StringUtils.isEmpty(secret.getTenantDomain())) {
            secret.setTenantDomain(getTenantDomain());
        }
    }

    /**
     * Validate that secret name. Validate the secret existence.
     * Set tenant domain if it is not set to the secret object.
     *
     * @param secret The secret to be replaced.
     * @throws SecretManagementException If secret validation fails.
     */
    private void validateSecretReplaceRequest(Secret secret)
            throws SecretManagementException {

        if (StringUtils.isEmpty(secret.getSecretName()) || StringUtils.isEmpty(secret.getValue())) {
            throw handleClientException(ERROR_CODE_SECRET_REPLACE_REQUEST_INVALID, null);
        }

        if (!isSecretExist(secret.getSecretName())) {
            throw handleClientException(ERROR_CODE_SECRET_DOES_NOT_EXISTS, secret.getSecretName());
        }

        if (StringUtils.isEmpty(secret.getTenantDomain())) {
            secret.setTenantDomain(getTenantDomain());
        }
    }

    /**
     * Select highest priority Secret DAO from an already sorted list of Secret DAOs.
     *
     * @return Highest priority Secret DAO.
     */
    private SecretDAO getSecretDAO() throws SecretManagementException {

        if (!this.secretDAOS.isEmpty()) {
            return secretDAOS.get(secretDAOS.size() - 1);
        } else {
            throw handleServerException(ERROR_CODE_GET_DAO, "secretDAOs");
        }
    }

    private int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    private String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    private boolean isSecretExist(String secretName) throws SecretManagementException {

        try {
            getSecret(secretName);
        } catch (SecretManagementClientException e) {
            if (ERROR_CODE_SECRET_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
        return true;
    }

    private boolean isSecretExistsById(String secretId) throws SecretManagementException {

        if (StringUtils.isBlank(secretId)) {
            throw handleClientException(ERROR_CODE_INVALID_SECRET_ID, secretId);
        }
        return this.getSecretDAO().isExistingSecret(getTenantId(), secretId);
    }

    private String generateSecretId(String secretName) throws SecretManagementException {

        String secretId;
        if (isSecretExist(secretName)) {
            secretId = getSecret(secretName).getSecretId();
        } else {
            secretId = generateUniqueID();
            if (log.isDebugEnabled()) {
                log.debug("Secret id generated: " + secretId);
            }
        }
        return secretId;
    }
}
