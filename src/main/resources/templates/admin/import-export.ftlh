<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Import / Export">
<h1 class="h3 mb-4">Import / Export</h1>
<#if importProblem??><div class="alert alert-danger">Import failed: ${importProblem}</div></#if>
<#if RequestParameters.clientsImported??><div class="alert alert-success">Imported ${RequestParameters.clientsImported} client(s).</div></#if>
<#if RequestParameters.mattersImported??><div class="alert alert-success">Imported ${RequestParameters.mattersImported} matter(s).</div></#if>

<div class="row g-4">
  <div class="col-md-6">
    <div class="card"><div class="card-body">
      <h2 class="h6">Import Clients (CSV)</h2>
      <p class="small text-muted">Columns: name,type(PERSON|ORGANIZATION),email,phone,portalUserEmail</p>
      <form method="post" action="/admin/import/clients" enctype="multipart/form-data">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <input class="form-control mb-2" type="file" name="file" accept=".csv" required>
        <button class="btn btn-primary" type="submit">Import Clients</button>
      </form>
    </div></div>
  </div>
  <div class="col-md-6">
    <div class="card"><div class="card-body">
      <h2 class="h6">Import Matters (CSV)</h2>
      <p class="small text-muted">Columns: title,clientName,practiceAreaName</p>
      <form method="post" action="/admin/import/matters" enctype="multipart/form-data">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <input class="form-control mb-2" type="file" name="file" accept=".csv" required>
        <button class="btn btn-primary" type="submit">Import Matters</button>
      </form>
    </div></div>
  </div>
</div>

<div class="card mt-4"><div class="card-body">
  <h2 class="h6">Import Matters (Court Docket XML)</h2>
  <p class="small text-muted">Elements: &lt;matter title="" client="" summary=""/&gt;</p>
  <form method="post" action="/admin/import/matters-xml" enctype="multipart/form-data">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <input class="form-control mb-2" type="file" name="file" accept=".xml" required>
    <button class="btn btn-primary" type="submit">Import Docket Extract</button>
  </form>
</div></div>

<div class="card mt-4"><div class="card-body">
  <h2 class="h6">Conflict Screen (Intake List)</h2>
  <p class="small text-muted">The first column of each row (client name) is checked against existing matters.</p>
  <#if RequestParameters.conflictsFlagged??><div class="alert alert-warning">${RequestParameters.conflictsFlagged} name(s) matched an existing matter.</div></#if>
  <form method="post" action="/intake/conflict-screen" enctype="multipart/form-data">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <input class="form-control mb-2" type="file" name="file" accept=".csv" required>
    <button class="btn btn-primary" type="submit">Run Conflict Screen</button>
  </form>
</div></div>

<div class="card mt-4"><div class="card-body">
  <h2 class="h6">Deadline Reminder Template</h2>
  <p class="small text-muted">Provide the wording used for deadline reminders. The file is used as the message template for every reminder the sweep emits (refer to <code>deadline</code> and <code>matter</code> in the wording).</p>
  <#if RequestParameters.noticeTemplateSaved??><div class="alert alert-success">Reminder template saved.</div></#if>
  <form method="post" action="/admin/notices/template" enctype="multipart/form-data" class="row g-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <div class="col-md-6"><input class="form-control" type="file" name="file" accept=".ftl" required></div>
    <div class="col-md-2"><button class="btn btn-primary w-100" type="submit">Save Template</button></div>
  </form>
</div></div>

<div class="card mt-4"><div class="card-body">
  <h2 class="h6">Export Reports (CSV)</h2>
  <a class="btn btn-outline-primary me-2" href="/admin/export/matters">Matter Status Report</a>
  <a class="btn btn-outline-primary me-2" href="/admin/export/deadlines">Deadlines Report</a>
  <a class="btn btn-outline-primary me-2" href="/admin/export/documents">Document Inventory Report</a>
  <a class="btn btn-outline-primary" href="/admin/export/integrations">Integration Inventory</a>
</div></div>
</@layout.page>
