<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Practice Areas">
<h1 class="h3 mb-4">Practice Areas</h1>
<div class="card mb-4"><div class="card-body">
  <form method="post" action="/admin/practice-areas" class="row g-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <div class="col-md-4"><input class="form-control" name="name" placeholder="Name" required></div>
    <div class="col-md-6"><input class="form-control" name="description" placeholder="Description"></div>
    <div class="col-md-2"><button class="btn btn-primary w-100" type="submit">Add</button></div>
  </form>
</div></div>
<div class="card">
  <table class="table table-hover mb-0">
    <thead><tr><th>Name</th><th>Description</th><th>Active</th><th></th></tr></thead>
    <tbody>
      <#list practiceAreas as pa>
        <tr>
          <td>${pa.name}</td><td>${(pa.description)!'-'}</td><td>${pa.active?string("Yes","No")}</td>
          <td>
            <form method="post" action="/admin/practice-areas/${pa.id}/toggle">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <button class="btn btn-sm btn-outline-secondary" type="submit">${pa.active?string("Deactivate","Activate")}</button>
            </form>
          </td>
        </tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
