<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title=matter.title>
<div class="d-flex justify-content-between align-items-start mb-3">
  <div>
    <h1 class="h3 mb-1">${matter.title}</h1>
    <div>${matter.client.name} &bull; <@layout.statusBadge status=matter.status/>
      <#if matter.status == "ARCHIVED"><span class="badge bg-secondary">Read-only</span></#if>
    </div>
  </div>
  <#if isStaff>
  <form method="post" action="/matters/${matter.id}/transition" class="d-flex gap-2">
    <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
    <#if matter.status == "INTAKE">
      <button class="btn btn-warning" name="target" value="CONFLICT_REVIEW">Run Conflict Check</button>
    </#if>
    <#if matter.status == "CONFLICT_REVIEW">
      <button class="btn btn-success" name="target" value="OPEN">Mark Open</button>
    </#if>
    <#if matter.status == "OPEN">
      <button class="btn btn-outline-warning" name="target" value="ON_HOLD">Put On Hold</button>
      <button class="btn btn-outline-dark" name="target" value="CLOSED">Close Matter</button>
    </#if>
    <#if matter.status == "ON_HOLD">
      <button class="btn btn-outline-success" name="target" value="OPEN">Reopen</button>
      <button class="btn btn-outline-dark" name="target" value="CLOSED">Close Matter</button>
    </#if>
    <#if matter.status == "CLOSED" && user.role == "FIRM_ADMIN">
      <button class="btn btn-outline-secondary" name="target" value="ARCHIVED">Archive</button>
    </#if>
  </form>
  </#if>
</div>

<ul class="nav nav-tabs mb-3">
  <#list ["overview","documents","tasks","deadlines","notes","messages","audit"] as t>
    <#if t != "notes" && t != "audit" || isStaff>
      <li class="nav-item"><a class="nav-link <#if tab==t>active</#if>" href="/matters/${matter.id}?tab=${t}">${t?cap_first}</a></li>
    </#if>
  </#list>
</ul>

<#if tab == "overview">
  <div class="card"><div class="card-body">
    <dl class="row">
      <dt class="col-sm-3">Description</dt><dd class="col-sm-9">${(matter.description)!'-'}</dd>
      <dt class="col-sm-3">Practice Area</dt><dd class="col-sm-9">${(matter.practiceArea.name)!'-'}</dd>
      <dt class="col-sm-3">Assigned Attorney</dt><dd class="col-sm-9">${(matter.assignedAttorney.fullName)!'Unassigned'}</dd>
      <dt class="col-sm-3">Sensitivity</dt><dd class="col-sm-9">${matter.sensitivityLevel}</dd>
      <dt class="col-sm-3">Opened</dt><dd class="col-sm-9">${(matter.openedDate?string)!'-'}</dd>
      <dt class="col-sm-3">Closed</dt><dd class="col-sm-9">${(matter.closedDate?string)!'-'}</dd>
    </dl>
    <a class="btn btn-sm btn-outline-secondary" href="/matters/case-ref/${matter.id}/card">Print Case Reference Card</a>
    <#if isFirmAdmin>
    <form method="post" action="/admin/matters/${matter.id}/reassign" class="row g-2 mt-3">
      <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
      <div class="col-md-6">
        <select class="form-select" name="attorneyId">
          <#list staffUsers as su><#if su.role == "ATTORNEY"><option value="${su.id}">${su.fullName}</option></#if></#list>
        </select>
      </div>
      <div class="col-md-3"><button class="btn btn-outline-secondary" type="submit">Reassign Matter</button></div>
    </form>
    </#if>
  </div></div>
  <div class="card mt-3"><div class="card-body">
    <h2 class="h6">Matter Assistant</h2>
    <form method="get" action="/matters/${matter.id}" class="row g-2">
      <input type="hidden" name="tab" value="overview"/>
      <div class="col-md-10"><input class="form-control" name="ask" placeholder="Ask about this matter" required></div>
      <div class="col-md-2"><button class="btn btn-primary w-100" type="submit">Ask</button></div>
    </form>
    <#if assistantAnswer??><div class="alert alert-secondary mt-3 mb-0">${assistantAnswer}</div></#if>
    <#if assistantProblem??><div class="alert alert-warning mt-3 mb-0">${assistantProblem}</div></#if>
  </div></div>
</#if>

<#if tab == "documents">
  <#if isStaff && matter.status != "ARCHIVED">
    <a class="btn btn-primary mb-3" href="/matters/${matter.id}/documents/upload">+ Upload Document</a>
  </#if>
  <div class="card">
    <table class="table table-hover mb-0">
      <thead><tr><th>Title</th><th>Category</th><th>Privilege</th><th>Visibility</th><th>Versions</th><th></th></tr></thead>
      <tbody>
        <#if documents?size == 0><tr><td colspan="6" class="empty-state">No documents.</td></tr></#if>
        <#list documents as d>
          <tr>
            <td>${d.title}</td>
            <td>${d.category?replace('_',' ')}</td>
            <td>${d.privilegeLevel?replace('_',' ')}</td>
            <td><@layout.statusBadge status=d.visibility/></td>
            <td>${d.versions?size}</td>
            <td><a class="btn btn-sm btn-outline-primary" href="/matters/${matter.id}/documents/${d.id}">Open</a></td>
          </tr>
        </#list>
      </tbody>
    </table>
  </div>
</#if>

<#if tab == "tasks">
  <#if isStaff>
  <div class="card mb-3"><div class="card-body">
    <h2 class="h6">New Task</h2>
    <form method="post" action="/matters/${matter.id}/tasks" class="row g-2">
      <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
      <div class="col-md-4"><input class="form-control" name="title" placeholder="Task title" required></div>
      <div class="col-md-3">
        <select class="form-select" name="assigneeId">
          <option value="">Unassigned</option>
          <#list staffUsers as su><option value="${su.id}">${su.fullName}</option></#list>
        </select>
      </div>
      <div class="col-md-2">
        <select class="form-select" name="priority">
          <option value="LOW">Low</option><option value="MEDIUM" selected>Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option>
        </select>
      </div>
      <div class="col-md-2"><input class="form-control" type="date" name="dueDate"></div>
      <div class="col-md-1"><button class="btn btn-primary w-100" type="submit">Add</button></div>
    </form>
  </div></div>
  </#if>
  <div class="card">
    <table class="table table-hover mb-0">
      <thead><tr><th>Title</th><th>Assignee</th><th>Priority</th><th>Due</th><th>Status</th><#if isStaff><th></th></#if></tr></thead>
      <tbody>
      <#if tasks?size == 0><tr><td colspan="6" class="empty-state">No tasks.</td></tr></#if>
      <#list tasks as t>
        <tr>
          <td>${t.title}</td>
          <td>${(t.assignee.fullName)!'Unassigned'}</td>
          <td>${t.priority}</td>
          <td>${(t.dueDate?string)!'-'}</td>
          <td><@layout.statusBadge status=t.status/></td>
          <#if isStaff>
          <td>
            <form method="post" action="/matters/${matter.id}/tasks/${t.id}/status" class="d-flex gap-1">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <select class="form-select form-select-sm" name="target">
                <#list ["OPEN","IN_PROGRESS","COMPLETED","BLOCKED","CANCELLED"] as s><option value="${s}">${s}</option></#list>
              </select>
              <button class="btn btn-sm btn-outline-secondary" type="submit">Update</button>
            </form>
          </td>
          </#if>
        </tr>
      </#list>
      </tbody>
    </table>
  </div>
</#if>

<#if tab == "deadlines">
  <#if isStaff>
  <div class="card mb-3"><div class="card-body">
    <h2 class="h6">New Deadline</h2>
    <form method="post" action="/matters/${matter.id}/deadlines" class="row g-2">
      <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
      <div class="col-md-5"><input class="form-control" name="title" placeholder="Deadline title" required></div>
      <div class="col-md-3"><input class="form-control" type="date" name="dueDate" required></div>
      <div class="col-md-3"><input class="form-control" name="rule" placeholder="Date rule (optional)"></div>
      <div class="col-md-3">
        <select class="form-select" name="responsibleId">
          <option value="">Unassigned</option>
          <#list staffUsers as su><option value="${su.id}">${su.fullName}</option></#list>
        </select>
      </div>
      <div class="col-md-1"><button class="btn btn-primary w-100" type="submit">Add</button></div>
    </form>
  </div></div>
  </#if>
  <div class="card">
    <table class="table table-hover mb-0">
      <thead><tr><th>Title</th><th>Due</th><th>Responsible</th><th>Completed</th><#if isStaff><th></th></#if></tr></thead>
      <tbody>
      <#if deadlines?size == 0><tr><td colspan="5" class="empty-state">No deadlines.</td></tr></#if>
      <#list deadlines as d>
        <tr>
          <td>${d.title}</td>
          <td>${d.dueDate}</td>
          <td>${(d.responsible.fullName)!'Unassigned'}</td>
          <td>${d.completed?string("Yes","No")}</td>
          <#if isStaff>
          <td>
            <#if !d.completed>
            <form method="post" action="/matters/${matter.id}/deadlines/${d.id}/complete">
              <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
              <button class="btn btn-sm btn-outline-success" type="submit">Mark complete</button>
            </form>
            </#if>
          </td>
          </#if>
        </tr>
      </#list>
      </tbody>
    </table>
  </div>
</#if>

<#if tab == "notes" && isStaff>
  <div class="card mb-3"><div class="card-body">
    <h2 class="h6">Add Privileged Note</h2>
    <form method="post" action="/matters/${matter.id}/notes">
      <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
      <textarea class="form-control mb-2" name="body" rows="2" required placeholder="Internal note - never visible to clients"></textarea>
      <button class="btn btn-primary" type="submit">Add Note</button>
    </form>
  </div></div>
  <#list notes as n>
    <div class="card mb-2"><div class="card-body">
      <p class="mb-1">${n.body}</p>
      <p class="small text-muted mb-0">${n.author.fullName} &bull; ${n.createdAt}</p>
    </div></div>
  </#list>
  <#if notes?size == 0><div class="empty-state">No privileged notes yet.</div></#if>
</#if>

<#if tab == "messages">
  <#list messages as msg>
    <div class="card mb-2"><div class="card-body">
      <p class="mb-1">${msg.body}</p>
      <p class="small text-muted mb-1">${msg.sender.fullName} &bull; ${msg.createdAt} &bull; <@layout.statusBadge status=msg.status/></p>
      <#if (msg.attachmentName)?? && msg.attachmentName?has_content>
        <p class="small mb-1"><a href="/matters/${matter.id}/messages/${msg.id}/attachment">${msg.attachmentName}</a></p>
      </#if>
      <#if isStaff && msg.status == "DRAFT">
        <form method="post" action="/matters/${matter.id}/messages/${msg.id}/approve" class="d-inline">
          <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
          <button class="btn btn-sm btn-outline-success" name="target" value="APPROVED">Approve</button>
          <button class="btn btn-sm btn-primary" name="target" value="SENT">Approve &amp; Send</button>
        </form>
      </#if>
    </div></div>
  </#list>
  <#if messages?size == 0><div class="empty-state">No messages yet.</div></#if>
  <div class="card mt-3"><div class="card-body">
    <h2 class="h6">Send a message</h2>
    <form method="post" action="/matters/${matter.id}/messages" enctype="multipart/form-data">
      <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
      <textarea class="form-control mb-2" name="body" rows="2" required></textarea>
      <input class="form-control mb-2" type="file" name="attachment">
      <button class="btn btn-primary" type="submit">Send</button>
    </form>
  </div></div>
</#if>

<#if tab == "audit" && isStaff>
  <div class="card">
    <ul class="list-group list-group-flush">
      <#list auditHistory as a>
        <li class="list-group-item small">
          <strong>${a.action}</strong> <span class="text-muted">by ${(a.actor.fullName)!'system'} at ${a.timestamp}</span>
          <#if a.beforeState?? || a.afterState??> &mdash; ${(a.beforeState)!''} &rarr; ${(a.afterState)!''}</#if>
        </li>
      </#list>
      <#if auditHistory?size == 0><li class="list-group-item empty-state">No audit history.</li></#if>
    </ul>
  </div>
</#if>
</@layout.page>
