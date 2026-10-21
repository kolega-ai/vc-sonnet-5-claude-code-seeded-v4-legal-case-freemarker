<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Matters">
<div class="d-flex justify-content-between align-items-center mb-3">
  <h1 class="h3">Matters</h1>
  <#if user.role != "CLIENT">
    <a class="btn btn-primary" href="/matters/new">+ New Matter</a>
  </#if>
</div>
<form class="row g-2 mb-3" method="get" action="/matters">
  <div class="col-auto">
    <select class="form-select" name="status">
      <option value="">All statuses</option>
      <#list statuses as s>
        <option value="${s}" <#if RequestParameters.status?? && RequestParameters.status == s?string>selected</#if>>${s?replace('_',' ')}</option>
      </#list>
    </select>
  </div>
  <#if user.role != "CLIENT">
  <div class="col-auto">
    <select class="form-select" name="practiceAreaId">
      <option value="">All practice areas</option>
      <#list practiceAreas as pa>
        <option value="${pa.id}">${pa.name}</option>
      </#list>
    </select>
  </div>
  </#if>
  <#if user.role == "FIRM_ADMIN">
  <div class="col-auto">
    <select class="form-select" name="attorneyId">
      <option value="">All attorneys</option>
      <#list attorneys as a>
        <option value="${a.id}">${a.fullName}</option>
      </#list>
    </select>
  </div>
  <div class="col-auto">
    <select class="form-select" name="clientId">
      <option value="">All clients</option>
      <#list clients as c>
        <option value="${c.id}">${c.name}</option>
      </#list>
    </select>
  </div>
  </#if>
  <div class="col-auto">
    <select class="form-select" name="sort">
      <option value="TITLE">Sort: Title</option>
      <option value="STATUS">Sort: Status</option>
      <option value="OPENED_DATE">Sort: Opened date</option>
      <option value="CLIENT">Sort: Client</option>
    </select>
  </div>
  <div class="col-auto"><button class="btn btn-outline-secondary" type="submit">Filter</button></div>
</form>
<div class="card">
  <div class="table-responsive">
  <table class="table table-hover mb-0">
    <thead><tr><th>Title</th><th>Client</th><th>Practice Area</th><th>Attorney</th><th>Status</th><th></th></tr></thead>
    <tbody>
      <#if matters?size == 0>
        <tr><td colspan="6" class="empty-state">No matters match your filters.</td></tr>
      </#if>
      <#list matters as m>
        <tr>
          <td>${m.title}</td>
          <td>${m.client.name}</td>
          <td>${(m.practiceArea.name)!'-'}</td>
          <td>${(m.assignedAttorney.fullName)!'Unassigned'}</td>
          <td><@layout.statusBadge status=m.status/></td>
          <td><a class="btn btn-sm btn-outline-primary" href="/matters/${m.id}">Open</a></td>
        </tr>
      </#list>
    </tbody>
  </table>
  </div>
</div>
</@layout.page>
