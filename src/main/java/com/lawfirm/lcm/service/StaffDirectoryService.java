package com.lawfirm.lcm.service;

import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

// Resolves staff against the firm's office directory (Active Directory/LDAP) so the
// onboarding integration can prefill the new user's name from the authoritative record.
@Service
public class StaffDirectoryService {

    private static final String DIRECTORY_URL = "ldap://directory.lawfirm.internal:389";
    private static final String PEOPLE_BASE = "ou=people,dc=lawfirm,dc=internal";

    public List<String> resolve(String mail) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, DIRECTORY_URL);
        DirContext ctx = new InitialDirContext(env);
        try {
            String filter = "(&(objectClass=person)(mail=" + mail + "))";
            NamingEnumeration<SearchResult> found = ctx.search(PEOPLE_BASE, filter, new SearchControls());
            List<String> people = new ArrayList<>();
            while (found.hasMore()) {
                Attribute cn = found.next().getAttributes().get("cn");
                if (cn != null) people.add(String.valueOf(cn.get()));
            }
            return people;
        } finally {
            ctx.close();
        }
    }
}
