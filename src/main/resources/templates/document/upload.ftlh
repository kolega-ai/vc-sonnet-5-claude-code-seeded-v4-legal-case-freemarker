<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Upload Document">
<h1 class="h3 mb-4">Upload Document &mdash; ${matter.title}</h1>
<div class="card"><div class="card-body">
<form method="post" action="/matters/${matter.id}/documents" enctype="multipart/form-data">
  <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
  <div class="mb-3"><label class="form-label" for="title">Title</label><input class="form-control" id="title" name="title" required></div>
  <div class="mb-3"><label class="form-label" for="category">Category</label>
    <select class="form-select" id="category" name="category">
      <#list categories as c><option value="${c}">${c?replace('_',' ')}</option></#list>
    </select>
  </div>
  <div class="mb-3"><label class="form-label" for="privilegeLevel">Privilege Level</label>
    <select class="form-select" id="privilegeLevel" name="privilegeLevel">
      <#list privilegeLevels as p><option value="${p}">${p?replace('_',' ')}</option></#list>
    </select>
  </div>
  <div class="mb-3"><label class="form-label" for="file">File</label><input class="form-control" id="file" name="file" type="file" required></div>
  <button class="btn btn-primary" type="submit">Upload</button>
</form>
</div></div>
</@layout.page>
