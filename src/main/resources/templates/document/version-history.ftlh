<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title=document.title>
<h1 class="h3 mb-2">${document.title}</h1>
<p class="text-muted">${matter.title} &bull; ${document.category?replace('_',' ')} &bull; ${document.privilegeLevel?replace('_',' ')} &bull; <@layout.statusBadge status=document.visibility/></p>

<#if isStaff>
<div class="card mb-3"><div class="card-body">
  <h2 class="h6">Sharing / Visibility</h2>
  <form method="post" action="/matters/${matter.id}/documents/${document.id}/visibility" class="d-flex gap-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <input type="hidden" name="continueTo" value="${(RequestParameters.back)!('/matters/' + matter.id + '?tab=documents')}"/>
    <select class="form-select" name="visibility" style="max-width:260px;">
      <#list visibilities as v><option value="${v}" <#if v==document.visibility>selected</#if>>${v?replace('_',' ')}</option></#list>
    </select>
    <button class="btn btn-outline-primary" type="submit">Change Visibility</button>
  </form>
  <p class="small text-muted mt-2 mb-0">Changing visibility is recorded in the audit log.</p>
</div></div>

<div class="card mb-3"><div class="card-body">
  <h2 class="h6">Upload New Version</h2>
  <form method="post" action="/matters/${matter.id}/documents/${document.id}/versions" enctype="multipart/form-data" class="row g-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <div class="col-md-6"><input class="form-control" type="file" name="file" required></div>
    <div class="col-md-4"><input class="form-control" name="notes" placeholder="Version notes"></div>
    <div class="col-md-2"><button class="btn btn-primary w-100" type="submit">Upload</button></div>
  </form>
</div></div>

<div class="card mb-3"><div class="card-body">
  <h2 class="h6">File a Version from a Court / Counsel Link</h2>
  <form method="post" action="/matters/${matter.id}/documents/${document.id}/versions/from-link" class="row g-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <div class="col-md-6"><input class="form-control" name="sourceUrl" placeholder="https://courts.example/filings/1234.pdf" required></div>
    <div class="col-md-4"><input class="form-control" name="notes" placeholder="Version notes"></div>
    <div class="col-md-2"><button class="btn btn-primary w-100" type="submit">Retrieve</button></div>
  </form>
</div></div>

<div class="card mb-3"><div class="card-body">
  <h2 class="h6">File a Converted Copy</h2>
  <p class="small text-muted mb-2">Renders the latest version to the format below with the office converter and files the result as a new version.</p>
  <button type="button" class="btn btn-outline-secondary" onclick="renderCopy('pdf')">Render PDF Copy</button>
  <button type="button" class="btn btn-outline-secondary" onclick="renderCopy('docx')">Render DOCX Copy</button>
  <script>
  function renderCopy(fmt) {
    var x = new XMLHttpRequest();
    x.open('POST', '/matters/${matter.id}/documents/${document.id}/versions/convert', true);
    x.setRequestHeader('${csrfToken.headerName}', '${csrfToken.token}');
    x.setRequestHeader('X-Convert-Format', fmt);
    x.onload = function () { window.location = '/matters/${matter.id}/documents/${document.id}'; };
    x.send();
  }
  </script>
</div></div>

<div class="card mb-3"><div class="card-body">
  <h2 class="h6">Court Bundle</h2>
  <p class="small text-muted mb-2">Stamps the latest version with your saved stamp profile and files it as the submission copy.</p>
  <form method="post" action="/matters/${matter.id}/documents/${document.id}/bundle">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <button class="btn btn-outline-secondary" type="submit">Assemble Bundle</button>
  </form>
</div></div>
</#if>

<div class="card">
  <div class="card-header">Version History</div>
  <table class="table table-hover mb-0">
    <thead><tr><th>Version</th><th>Filename</th><th>Uploaded By</th><th>Uploaded At</th><th>Notes</th><th></th></tr></thead>
    <tbody>
      <#list document.versions as v>
        <tr>
          <td>v${v.versionNumber}</td>
          <td>${v.originalFilename}</td>
          <td>${(v.uploadedBy.fullName)!'-'}</td>
          <td>${v.uploadedAt}</td>
          <td>${(v.notes)!'-'}</td>
          <td><a class="btn btn-sm btn-outline-primary" href="/matters/${matter.id}/documents/${document.id}/versions/${v.id}/download">Download</a></td>
        </tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
