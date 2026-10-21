<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="User Management">
<h1 class="h3 mb-4">User Management</h1>
<div class="card mb-4"><div class="card-body">
  <h2 class="h6">Invite New User</h2>
  <form method="post" action="/admin/users" class="row g-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <div class="col-md-4"><input class="form-control" name="email" type="email" placeholder="Email" required></div>
    <div class="col-md-4"><input class="form-control" name="fullName" placeholder="Full name" required></div>
    <div class="col-md-3">
      <select class="form-select" name="role">
        <#list roles as r><option value="${r}">${r}</option></#list>
      </select>
    </div>
    <div class="col-md-1"><button class="btn btn-primary w-100" type="submit">Add</button></div>
  </form>
  <p class="small text-muted mt-2 mb-0">New users get the default demo password (see README) and should change it after first login.</p>
</div></div>
<div class="card">
  <table class="table table-hover mb-0">
    <thead><tr><th>Email</th><th>Full Name</th><th>Role</th><th>Enabled</th><th></th></tr></thead>
    <tbody>
      <#list users as u>
        <tr>
          <td>${u.email}</td>
          <td>${u.fullName}</td>
          <td>
            <form method="post" action="/admin/users/${u.id}/role" class="d-flex gap-1">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <select class="form-select form-select-sm" name="role">
                <#list roles as r><option value="${r}" <#if r==u.role>selected</#if>>${r}</option></#list>
              </select>
              <button class="btn btn-sm btn-outline-secondary" type="submit">Set</button>
            </form>
          </td>
          <td>${u.enabled?string("Yes","No")}</td>
          <td>
            <form method="post" action="/admin/users/${u.id}/enabled">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <input type="hidden" name="enabled" value="${u.enabled?string("false","true")}"/>
              <button class="btn btn-sm btn-outline-warning" type="submit">${u.enabled?string("Disable","Enable")}</button>
            </form>
          </td>
        </tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
