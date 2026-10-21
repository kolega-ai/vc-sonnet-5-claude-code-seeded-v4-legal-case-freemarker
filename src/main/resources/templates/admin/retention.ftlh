<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Retention Settings">
<h1 class="h3 mb-4">Retention Settings</h1>
<#if RequestParameters.updated??><div class="alert alert-success">Settings updated.</div></#if>
<div class="card"><div class="card-body">
<form method="post" action="/admin/retention">
  <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
  <div class="mb-3">
    <label class="form-label" for="documentRetentionDays">Document retention (days)</label>
    <input class="form-control" id="documentRetentionDays" name="documentRetentionDays" type="number" min="1" value="${retentionForm.documentRetentionDays}">
  </div>
  <div class="mb-3">
    <label class="form-label" for="matterRetentionDays">Matter retention (days)</label>
    <input class="form-control" id="matterRetentionDays" name="matterRetentionDays" type="number" min="1" value="${retentionForm.matterRetentionDays}">
  </div>
  <div class="mb-3">
    <label class="form-label" for="deadlineReminderDays">Deadline reminder lead time (days)</label>
    <input class="form-control" id="deadlineReminderDays" name="deadlineReminderDays" type="number" min="1" value="${retentionForm.deadlineReminderDays}">
  </div>
  <button class="btn btn-primary" type="submit">Save Settings</button>
</form>
</div></div>
</@layout.page>
