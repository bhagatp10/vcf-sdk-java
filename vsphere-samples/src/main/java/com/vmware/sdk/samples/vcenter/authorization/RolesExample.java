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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sdk.samples.utils.SampleCommandLineParser;
import com.vmware.sdk.vsphere.utils.VcenterClient;
import com.vmware.sdk.vsphere.utils.VcenterClientFactory;
import com.vmware.vcenter.authorization.Roles;
import com.vmware.vcenter.authorization.RolesTypes;

/**
 * This example demonstrates how to create, update, delete and get authorization roles from a vCenter using the
 * corresponding vCenter REST APIs.
 *
 * <p>This sample requires a single vCenter and should work from vCenter 9.0 onwards.
 */
public class RolesExample {

    private static final Logger log = LoggerFactory.getLogger(RolesExample.class);

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
        SampleCommandLineParser.load(RolesExample.class, args);

        VcenterClientFactory factory =
                new VcenterClientFactory(serverAddress, loadKeystoreOrCreateEmpty(trustStorePath));

        try (VcenterClient client = factory.createClient(username, password, null)) {
            // Create role stub
            Roles rolesStub = client.createStub(Roles.class);

            // List all roles in the vCenter
            RolesTypes.ListResult roles = getAllRoles(rolesStub);

            // Get the details for each role
            for (RolesTypes.ListItem item : roles.getItems()) {
                getRoleDetails(rolesStub, item.getRole());
            }

            // Create a new role
            String uuid = UUID.randomUUID().toString();
            String newRoleName = "test-role-" + uuid;
            Set<String> privs = new HashSet<>(Arrays.asList("System.View", "Global.LogEvent"));
            String newRoleId = createRole(rolesStub, newRoleName, "text", privs);

            // Getting the details of the newly created role
            getRoleDetails(rolesStub, newRoleId);

            // Updating the role, adding a new privilege
            privs.add("Global.Settings");
            updateRole(rolesStub, newRoleId, privs);

            // Getting the details of the newly updated role
            getRoleDetails(rolesStub, newRoleId);

            // Delete the role
            deleteRole(rolesStub, newRoleId);

            // Try to get the deleted role to make sure it no longer exists
            try {
                getRoleDetails(rolesStub, newRoleId);
            } catch (com.vmware.vapi.std.errors.NotFound e) {
                log.info("Role deletion confirmed");
            }
        }
    }

    private static final void updateRole(Roles rolesStub, String roleId, Set<String> newPrivileges) {
        log.info("Updating role with ID {}", roleId);
        RolesTypes.UpdateSpec spec = new RolesTypes.UpdateSpec();
        spec.setPrivileges(newPrivileges);
        rolesStub.update(roleId, spec);
    }

    private static final void deleteRole(Roles rolesStub, String roleId) {
        log.info("Deleting a role with ID {}", roleId);
        rolesStub.delete(roleId);
    }

    private static final RolesTypes.ListResult getAllRoles(Roles rolesStub) {
        log.info("Getting all roles in the vCenter");
        Roles.ListResult rolesList = rolesStub.list(null, null);
        parseListResult(rolesList);
        return rolesList;
    }

    private static final void getRoleDetails(Roles rolesStub, String roleId) {
        log.info("Getting the details of role with ID {}", roleId);
        RolesTypes.Info info = rolesStub.get(roleId);
        log.info("Details: {}", info);
    }

    private static final String createRole(
            Roles rolesStub, String roleName, String roleDescription, Set<String> privileges) {
        log.info("Creating a new role with name {}", roleName);
        RolesTypes.CreateSpec spec = new RolesTypes.CreateSpec();
        spec.setName(roleName);
        spec.setDescription(roleDescription);
        spec.setPrivileges(privileges);
        String newRoleId = rolesStub.create(spec);
        log.info("Created new role with ID: {}", newRoleId);
        return newRoleId;
    }

    private static final void parseListResult(RolesTypes.ListResult rolesList) {
        for (RolesTypes.ListItem role : rolesList.getItems()) {
            log.info("ID = {}", role.getRole());
            log.info("Name = {}", role.getInfo().getName());
            log.info("Description = {}", role.getInfo().getDescription());
            log.info("IsSystem = {}", role.getInfo().getSystem());
            log.info("Privileges = {}\n", role.getInfo().getPrivileges());
        }
    }
}
