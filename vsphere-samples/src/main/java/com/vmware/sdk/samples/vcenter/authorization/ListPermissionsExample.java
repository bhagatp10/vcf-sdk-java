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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.std.DynamicID;
import com.vmware.vcenter.authorization.Permissions;
import com.vmware.vcenter.authorization.PermissionsTypes;

/**
 * This example demonstrates how to query global and inventory permissions from a vCenter using the corresponding
 * vCenter REST APIs.
 *
 * <p>This sample requires a single vCenter and should work from vCenter 9.0 onwards.
 */
public class ListPermissionsExample {

    private static final Logger log = LoggerFactory.getLogger(ListPermissionsExample.class);

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
        SampleCommandLineParser.load(ListPermissionsExample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            // Create permission stub
            Permissions permissionStub = client.createStub(Permissions.class);

            // List all permissions in the vCenter
            Permissions.ListResult allPerms = getAllPermissions(permissionStub);

            // List only global permissions
            getGlobalPermissionsOnly(permissionStub);

            // List only inventory permissions
            getRootFolderPermissionsOnly(permissionStub);

            // Getting details for each permission
            for (PermissionsTypes.ListItem permission : allPerms.getItems()) {
                getPermissionDetails(permissionStub, permission.getPermission());
            }
        }
    }

    private static final PermissionsTypes.ListResult getAllPermissions(Permissions permissionStub) {
        log.info("Getting all permissions in the vCenter");
        PermissionsTypes.ListResult permissionsList = permissionStub.list(null, null);
        parseListResult(permissionsList);
        return permissionsList;
    }

    private static final void getGlobalPermissionsOnly(Permissions permissionStub) {
        log.info("Getting only global permissions in the vCenter");
        PermissionsTypes.FilterSpec fs = new PermissionsTypes.FilterSpec();
        DynamicID globalPerm = new DynamicID();
        globalPerm.setType("GlobalAcl");
        globalPerm.setId("GlobalAcl");
        List<DynamicID> objs = new ArrayList<>(Arrays.asList(globalPerm));
        fs.setObjects(objs);
        parseListResult(permissionStub.list(fs, null));
    }

    private static final void getRootFolderPermissionsOnly(Permissions permissionStub) {
        log.info("Getting inventory permissions on vCenter's root folder only");
        PermissionsTypes.FilterSpec fs = new PermissionsTypes.FilterSpec();
        DynamicID rootFolderPerm = new DynamicID();
        rootFolderPerm.setType("ManagedEntity");
        rootFolderPerm.setId("group-d1");
        List<DynamicID> objs = new ArrayList<>(Arrays.asList(rootFolderPerm));
        fs.setObjects(objs);
        parseListResult(permissionStub.list(fs, null));
    }

    private static final void parseListResult(PermissionsTypes.ListResult permissionsList) {
        for (PermissionsTypes.ListItem permission : permissionsList.getItems()) {
            if (permission.getInfo().getObject().getType().equals("GlobalAcl")) {
                log.info("============ Global Permission ============");
            } else {
                log.info("============ Inventory Permission ============");
            }
            log.info("ID = {}", permission.getPermission());
            log.info("Principal = {}", permission.getInfo().getPrincipal());
            log.info("Role ID = {}", permission.getInfo().getRole());
            log.info("isPropagating = {}\n", permission.getInfo().getPropagating());
        }
    }

    private static final void getPermissionDetails(Permissions permissionStub, String permissionId) {
        log.info("Getting permission details");
        PermissionsTypes.Info info = permissionStub.get(permissionId);
        log.info("Permission = {}", info);
    }
}
