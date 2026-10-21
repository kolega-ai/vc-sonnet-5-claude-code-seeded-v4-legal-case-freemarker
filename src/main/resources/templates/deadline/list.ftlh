<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="Deadlines">
<div class="d-flex justify-content-between align-items-center mb-3">
  <h1 class="h3">Deadlines &amp; Calendar</h1>
  <#if user.role == "FIRM_ADMIN">
  <form method="post" action="/admin/deadlines/run-reminders">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <button class="btn btn-outline-warning" type="submit">Run reminder sweep now</button>
  </form>
  </#if>
</div>
<#if RequestParameters.ran??><div class="alert alert-info">Reminder sweep sent ${RequestParameters.ran} reminder(s).</div></#if>
<div class="card">
  <table class="table table-hover mb-0">
    <thead><tr><th>Title</th><th>Matter</th><th>Due Date</th><th>Responsible</th><th>Completed</th><th>Reminder Sent</th></tr></thead>
    <tbody>
      <#if deadlines?size == 0><tr><td colspan="6" class="empty-state">No deadlines.</td></tr></#if>
      <#list deadlines as d>
        <tr>
          <td><a href="/matters/${d.matter.id}?tab=deadlines">${d.title}</a></td>
          <td>${d.matter.title}</td>
          <td>${d.dueDate}</td>
          <td>${(d.responsible.fullName)!'Unassigned'}</td>
          <td>
            <#if d.completed>Yes<#elseif user.role != "CLIENT">
              <a href="/matters/${d.matter.id}/deadlines/${d.id}/complete">Mark done</a>
            <#else>No</#if>
          </td>
          <td>${(d.reminderSentAt?string)!'-'}</td>
        </tr>
      </#list>
    </tbody>
  </table>
</div>
</@layout.page>
