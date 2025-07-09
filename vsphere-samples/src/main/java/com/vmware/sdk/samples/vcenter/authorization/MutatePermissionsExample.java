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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vapi.std.DynamicID;
import com.vmware.vcenter.authorization.Permissions;
import com.vmware.vcenter.authorization.PermissionsTypes;

/**
 * This example demonstrates how to create, update and delete global and inventory permissions from a vCenter using the
 * corresponding vCenter REST APIs.
 *
 * <p>This sample requires a single vCenter and should work from vCenter 9.0 onwards.
 */
public class MutatePermissionsExample {

    private static final Logger log = LoggerFactory.getLogger(MutatePermissionsExample.class);

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
        SampleCommandLineParser.load(MutatePermissionsExample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            // Create permission stub
            Permissions permissionStub = client.createStub(Permissions.class);

            // Make examples with inventory permissions
            exampleInventoryPermissions(permissionStub);

            // Make examples with global permissions
            exampleGlobalPermissions(permissionStub);
        }
    }

    private static final void exampleGlobalPermissions(Permissions permissionStub) {
        // Create a new global permission for a person user
        String uuid = UUID.randomUUID().toString();
        String userName = "test-user-" + uuid;
        String domainName = "test.domain." + uuid;
        String newGlobPermId = createGlobalPermission(permissionStub, userName, domainName, true);

        // Gets the details of the newly created global permission
        getPermissionDetails(permissionStub, newGlobPermId);

        // Updates the roles of the newly created global permission
        updatePermissionRole(permissionStub, newGlobPermId, "-2");

        // Gets the details of the newly updated global permission
        getPermissionDetails(permissionStub, newGlobPermId);

        // Delete the global permission
        deletePermission(permissionStub, newGlobPermId);

        // Try to get the deleted permission to make sure it no longer exists
        try {
            getPermissionDetails(permissionStub, newGlobPermId);
        } catch (com.vmware.vapi.std.errors.NotFound e) {
            log.info("Global permission deletion confirmed");
        }
    }

    private static final void exampleInventoryPermissions(Permissions permissionStub) {
        // Create a new inventory permission for a person user
        String uuid = UUID.randomUUID().toString();
        String userName = "test-user-" + uuid;
        String domainName = "test.domain." + uuid;
        String newInvtPermId = createInventoryPermission(permissionStub, userName, domainName, false);

        // Gets the details of the newly created inventory permission
        getPermissionDetails(permissionStub, newInvtPermId);

        // Updates the roles of the newly created inventory permission
        updatePermissionRole(permissionStub, newInvtPermId, "-2");

        // Gets the details of the newly updated inventory permission
        getPermissionDetails(permissionStub, newInvtPermId);

        // Delete the inventory permission
        deletePermission(permissionStub, newInvtPermId);

        // Try to get the deleted permission to make sure it no longer exists
        try {
            getPermissionDetails(permissionStub, newInvtPermId);
        } catch (com.vmware.vapi.std.errors.NotFound e) {
            log.info("Inventory permission deletion confirmed");
        }
    }

    private static final void deletePermission(Permissions permissionStub, String permissionId) {
        log.info("Deleting permission with ID {}", permissionId);
        permissionStub.delete(permissionId);
    }

    private static final void updatePermissionRole(Permissions permissionStub, String permissionId, String roleId) {
        log.info("Updating permission with ID {}", permissionId);
        PermissionsTypes.UpdateSpec spec = new PermissionsTypes.UpdateSpec();
        spec.setRole(roleId);
        permissionStub.update(permissionId, spec);
    }

    private static final String createInventoryPermission(
            Permissions permissionStub, String userName, String domainName, boolean isGroup) {
        log.info("Creating inventory permission for principal {}@{}", userName, domainName);
        PermissionsTypes.CreateSpec spec = new PermissionsTypes.CreateSpec();
        DynamicID globalPerm = new DynamicID();
        globalPerm.setType("ManagedEntity");
        globalPerm.setId("group-d1");
        spec.setObject(globalPerm);
        PermissionsTypes.Principal principal = new PermissionsTypes.Principal();
        if (isGroup) {
            principal.setType(PermissionsTypes.Principal.Type.GROUP);
        } else {
            principal.setType(PermissionsTypes.Principal.Type.USER);
        }
        principal.setName(userName);
        principal.setDomain(domainName);
        spec.setPrincipal(principal);
        spec.setRole("-1");
        spec.setPropagating(true);
        String newPermId = permissionStub.create(spec);
        log.info("Inventory permission with ID {} created", newPermId);
        return newPermId;
    }

    private static final String createGlobalPermission(
            Permissions permissionStub, String userName, String domainName, boolean isGroup) {
        log.info("Creating global permission for principal {}@{}", userName, domainName);
        PermissionsTypes.CreateSpec spec = new PermissionsTypes.CreateSpec();
        DynamicID globalPerm = new DynamicID();
        globalPerm.setType("GlobalAcl");
        globalPerm.setId("GlobalAcl");
        spec.setObject(globalPerm);
        PermissionsTypes.Principal principal = new PermissionsTypes.Principal();
        if (isGroup) {
            principal.setType(Permissions.Principal.Type.GROUP);
        } else {
            principal.setType(Permissions.Principal.Type.USER);
        }
        principal.setName(userName);
        principal.setDomain(domainName);
        spec.setPrincipal(principal);
        spec.setRole("-1");
        spec.setPropagating(true);
        String newPermId = permissionStub.create(spec);
        log.info("Global permission with ID {} created", newPermId);
        return newPermId;
    }

    private static final void getPermissionDetails(Permissions permissionStub, String permissionId) {
        log.info("Getting permission details");
        PermissionsTypes.Info info = permissionStub.get(permissionId);
        log.info("Permission = {}", info);
    }
}
