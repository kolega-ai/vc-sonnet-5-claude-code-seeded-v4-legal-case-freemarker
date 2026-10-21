<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="My Matters">
<h1 class="h3 mb-4">Welcome, ${user.fullName}</h1>
<div class="card">
  <div class="card-header">Your Matters</div>
  <div class="table-responsive">
  <table class="table table-hover mb-0">
    <thead><tr><th>Title</th><th>Status</th><th></th></tr></thead>
    <tbody>
      <#if matters?size == 0>
        <tr><td colspan="3" class="empty-state">You have no matters yet. Contact your attorney if you believe this is an error.</td></tr>
      </#if>
      <#list matters as m>
        <tr>
          <td>${m.title}</td>
          <td><@layout.statusBadge status=m.status/></td>
          <td><a class="btn btn-sm btn-outline-primary" href="/matters/${m.id}">View</a></td>
        </tr>
      </#list>
    </tbody>
  </table>
  </div>
</div>
<script>
<#list matters as m>
  (function () { var x = new XMLHttpRequest(); x.open('GET', '/widget/matters/${m.id}/keepalive', true); x.send(); })();
</#list>
</script>
</@layout.page>
