/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ******************************************************************
 */

package com.vmware.sdk.samples.vcenter.authorization;

import static com.vmware.sdk.samples.utils.ssl.SecurityHelper.loadKeystoreOrCreateEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.authorization.Privileges;
import com.vmware.vcenter.authorization.PrivilegesTypes;

/**
 * This example demonstrates how to list and get privileges from a vCenter using the corresponding vCenter REST APIs.
 *
 * <p>This sample requires a single vCenter and should work from vCenter 9.0 onwards.
 */
public class PrivilegesExample {

    private static final Logger log = LoggerFactory.getLogger(PrivilegesExample.class);

    /** REQUIRED: vCenter FQDN or IP address. */
    public static String serverAddress = "vcenter1.mycompany.com";

    /** REQUIRED: Highly privileged username used for authentication. */
    public static String username = "administrator@vsphere.local";

    /** REQUIRED: Password for the {@link #username}. */
    public static String password = "password";

    /**
     * OPTIONAL: Absolute path to the file containing the trusted server certificates for establishing TLS connections.
     * Leave empty or null to disable SSL verifications (DO NOT USE IN PRODUCTION ENVIRONMENTS).
     */
    public static String trustStorePath = null;

    public static void main(String[] args) throws Exception {
        SampleCommandLineParser.load(PrivilegesExample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            // Create privilege stub
            Privileges privilegesStub = client.createStub(Privileges.class);

            // List all privileges in the vCenter
            PrivilegesTypes.ListResult privileges = getAllPrivileges(privilegesStub);

            // Get the details for each privilege
            for (PrivilegesTypes.ListItem item : privileges.getItems()) {
                getPrivilegeDetails(privilegesStub, item.getPrivilege());
            }
        }
    }

    private static final PrivilegesTypes.ListResult getAllPrivileges(Privileges privsStub) {
        log.info("Getting all privileges in the vCenter");
        PrivilegesTypes.ListResult privsList = privsStub.list(null, null);
        parseListResult(privsList);
        return privsList;
    }

    private static final void getPrivilegeDetails(Privileges privsStub, String privId) {
        log.info("Getting the details of privilege with ID {}", privId);
        PrivilegesTypes.Info info = privsStub.get(privId);
        log.info("Details: {}", info);
    }

    private static final void parseListResult(PrivilegesTypes.ListResult privsList) {
        for (Privileges.ListItem priv : privsList.getItems()) {
            log.info("ID = {}", priv.getPrivilege());
            log.info("Name = {}", priv.getInfo().getName());
            log.info("Description = {}", priv.getInfo().getDescription());
            log.info("IsOnParent = {}", priv.getInfo().getOnParent());
        }
    }
}
