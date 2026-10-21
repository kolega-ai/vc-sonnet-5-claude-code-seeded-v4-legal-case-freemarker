<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Audit Log">
<h1 class="h3 mb-4">Audit Log</h1>
<form class="row g-2 mb-3" method="get" action="/admin/audit-log">
  <div class="col-auto">
    <input class="form-control" name="entityType" placeholder="Filter by entity type (e.g. Matter)" value="${(entityType)!''}">
  </div>
  <div class="col-auto"><button class="btn btn-outline-secondary" type="submit">Filter</button></div>
</form>
<div class="card">
  <table class="table table-hover mb-0">
    <thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Entity</th><th>Before</th><th>After</th></tr></thead>
    <tbody>
      <#if logs?size == 0><tr><td colspan="6" class="empty-state">No matching audit entries.</td></tr></#if>
      <#list logs as l>
        <tr>
          <td>${l.timestamp}</td>
          <td>${(l.actor.fullName)!'system'}</td>
          <td>${l.action}</td>
          <td>${l.entityType}#${(l.entityId)!''}</td>
          <td>${(l.beforeState)!'-'}</td>
          <td>${(l.afterState)!'-'}</td>
        </tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
