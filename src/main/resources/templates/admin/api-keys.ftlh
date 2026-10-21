<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="API Keys">
<h1 class="h3 mb-4">API Keys</h1>
<p class="text-muted">Used by third-party integrations to call the read-only JSON API at <code>/api/v1/*</code> with header <code>X-Api-Key</code>.</p>
<div class="card mb-4"><div class="card-body">
  <form method="post" action="/admin/api-keys" class="row g-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <div class="col-md-4"><input class="form-control" name="name" placeholder="Key name / purpose" required></div>
    <div class="col-md-4"><input class="form-control" name="callbackUrl" placeholder="Status callback URL (optional)"></div>
    <div class="col-md-2"><button class="btn btn-primary w-100" type="submit">Generate</button></div>
  </form>
</div></div>
<div class="card">
  <table class="table table-hover mb-0">
    <thead><tr><th>Name</th><th>Key</th><th>Callback</th><th>Active</th><th>Created</th><th></th></tr></thead>
    <tbody>
      <#list apiKeys as k>
        <tr>
          <td>${k.name}</td>
          <td><code>${k.keyValue}</code></td>
          <td class="small">${(k.callbackUrl)!'-'}</td>
          <td>${k.active?string("Yes","No")}</td>
          <td>${k.createdAt}</td>
          <td>
            <#if k.active>
            <form method="post" action="/admin/api-keys/${k.id}/revoke">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <button class="btn btn-sm btn-outline-danger" type="submit">Revoke</button>
            </form>
            </#if>
          </td>
        </tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
