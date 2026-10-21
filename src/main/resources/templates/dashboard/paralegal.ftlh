<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Paralegal Dashboard">
<h1 class="h3 mb-4">Welcome, ${user.fullName}</h1>
<div class="row g-4">
  <div class="col-md-6">
    <div class="card">
      <div class="card-header">My Task Queue (${tasks?size})</div>
      <ul class="list-group list-group-flush">
        <#if tasks?size == 0><li class="list-group-item empty-state">No tasks assigned.</li></#if>
        <#list tasks as t>
          <li class="list-group-item d-flex justify-content-between">
            <span>${t.title} <span class="text-muted small">(${t.matter.title})</span></span>
            <@layout.statusBadge status=t.status/>
          </li>
        </#list>
      </ul>
    </div>
  </div>
  <div class="col-md-6">
    <div class="card">
      <div class="card-header">Upcoming Deadlines</div>
      <ul class="list-group list-group-flush">
        <#if deadlines?size == 0><li class="list-group-item empty-state">No deadlines.</li></#if>
        <#list deadlines as d>
          <li class="list-group-item d-flex justify-content-between">
            <span>${d.title} &mdash; ${d.matter.title}</span>
            <span class="text-muted small">${d.dueDate}</span>
          </li>
        </#list>
      </ul>
    </div>
  </div>
</div>
<p class="mt-4"><a class="btn btn-outline-primary" href="/matters">Browse matters &amp; upload documents</a></p>
</@layout.page>
