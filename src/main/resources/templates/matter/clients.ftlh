<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Clients">
<div class="d-flex justify-content-between align-items-center mb-3">
  <h1 class="h3">Clients</h1>
  <a class="btn btn-primary" href="/clients/new">+ New Client</a>
</div>
<div class="card">
  <table class="table table-hover mb-0">
    <thead><tr><th>Name</th><th>Type</th><th>Email</th><th>Phone</th><th>Status</th><th>Portal</th></tr></thead>
    <tbody>
      <#if clients?size == 0><tr><td colspan="6" class="empty-state">No clients yet.</td></tr></#if>
      <#list clients as c>
        <tr><td>${c.name}</td><td>${c.type}</td><td>${(c.email)!'-'}</td><td>${(c.phone)!'-'}</td><td><@layout.statusBadge status=c.status/></td>
          <td>
            <#if (c.email)?? && c.email?has_content>
            <form method="post" action="/clients/${c.id}/portal-invite">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <button class="btn btn-sm btn-outline-secondary" type="submit">Invite to portal</button>
            </form>
            </#if>
          </td></tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
