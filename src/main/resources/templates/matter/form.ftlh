<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="New Matter">
<h1 class="h3 mb-4">New Matter (Intake)</h1>
<div class="card"><div class="card-body">
<form method="post" action="/matters">
  <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
  <div class="mb-3">
    <label class="form-label" for="title">Title</label>
    <input class="form-control" id="title" name="title" required value="${(matterForm.title)!''}">
  </div>
  <div class="mb-3">
    <label class="form-label" for="description">Description</label>
    <textarea class="form-control" id="description" name="description" rows="3">${(matterForm.description)!''}</textarea>
  </div>
  <div class="row">
    <div class="col-md-4 mb-3">
      <label class="form-label" for="clientId">Client</label>
      <select class="form-select" id="clientId" name="clientId" required>
        <option value="">Select client</option>
        <#list clients as c><option value="${c.id}">${c.name}</option></#list>
      </select>
    </div>
    <div class="col-md-4 mb-3">
      <label class="form-label" for="practiceAreaId">Practice Area</label>
      <select class="form-select" id="practiceAreaId" name="practiceAreaId">
        <option value="">Unassigned</option>
        <#list practiceAreas as pa><option value="${pa.id}">${pa.name}</option></#list>
      </select>
    </div>
    <div class="col-md-4 mb-3">
      <label class="form-label" for="assignedAttorneyId">Assigned Attorney</label>
      <select class="form-select" id="assignedAttorneyId" name="assignedAttorneyId">
        <option value="">Unassigned</option>
        <#list attorneys as a><option value="${a.id}">${a.fullName}</option></#list>
      </select>
    </div>
  </div>
  <div class="mb-3">
    <label class="form-label" for="sensitivityLevel">Sensitivity Level</label>
    <select class="form-select" id="sensitivityLevel" name="sensitivityLevel">
      <option value="LOW">Low</option>
      <option value="MEDIUM">Medium</option>
      <option value="HIGH">High</option>
    </select>
  </div>
  <button class="btn btn-primary" type="submit">Create Matter (starts as Intake)</button>
</form>
</div></div>
</@layout.page>
