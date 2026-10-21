<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Firm Admin Dashboard">
<h1 class="h3 mb-4">Firm Overview</h1>
<div class="row g-4 mb-4">
  <div class="col-md-3"><div class="card text-center p-3"><div class="fs-2">${totalClients}</div><div class="text-muted">Clients</div></div></div>
  <div class="col-md-3"><div class="card text-center p-3"><div class="fs-2">${totalMatters}</div><div class="text-muted">Matters</div></div></div>
  <div class="col-md-3"><div class="card text-center p-3"><div class="fs-2">${pendingConflictReview?size}</div><div class="text-muted">Pending Conflict Review</div></div></div>
</div>
<div class="row g-4">
  <div class="col-md-6">
    <div class="card">
      <div class="card-header">Matters Awaiting Conflict Review</div>
      <ul class="list-group list-group-flush">
        <#if pendingConflictReview?size == 0><li class="list-group-item empty-state">Nothing pending.</li></#if>
        <#list pendingConflictReview as m>
          <li class="list-group-item"><a href="/matters/${m.id}">${m.title}</a> &mdash; ${m.client.name}</li>
        </#list>
      </ul>
    </div>
  </div>
  <div class="col-md-6">
    <div class="card">
      <div class="card-header">Recent Audit Activity</div>
      <ul class="list-group list-group-flush">
        <#list recentAudit as a>
          <li class="list-group-item small">
            <strong>${a.action}</strong> on ${a.entityType}#${(a.entityId)!''}
            <span class="text-muted">by ${(a.actor.fullName)!'system'} at ${a.timestamp}</span>
          </li>
        </#list>
      </ul>
    </div>
  </div>
</div>
</@layout.page>
