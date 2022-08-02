/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Error messages provided in adaptive script in retry page.
 */
public class AuthenticationError implements Serializable {

    private static final long serialVersionUID = -5291101013102578802L;

    private final Map<String, String> failureData;

    public AuthenticationError(Map<String, String> failureData) {

        this.failureData = failureData;
    }

    public Map<String, String> getFailureData() {

        return Collections.unmodifiableMap(failureData);
    }
}
