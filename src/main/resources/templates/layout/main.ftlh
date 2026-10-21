<#ftl output_format="HTML">
<#macro page title>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} - Legal Case Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/app.css?v=${assetVersion}" rel="stylesheet">
</head>
<body>
<#if user??>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark app-navbar">
  <div class="container-fluid">
    <a class="navbar-brand" href="/dashboard">Lex<span class="text-warning">Manage</span></a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMain">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navMain">
      <ul class="navbar-nav me-auto">
        <li class="nav-item"><a class="nav-link" href="/dashboard">Dashboard</a></li>
        <li class="nav-item"><a class="nav-link" href="/matters">Matters</a></li>
        <#if user.role != "CLIENT">
        <li class="nav-item"><a class="nav-link" href="/clients">Clients</a></li>
        <li class="nav-item"><a class="nav-link" href="/deadlines">Deadlines</a></li>
        </#if>
        <#if user.role == "FIRM_ADMIN">
        <li class="nav-item dropdown">
          <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">Admin</a>
          <ul class="dropdown-menu">
            <li><a class="dropdown-item" href="/admin/users">Users</a></li>
            <li><a class="dropdown-item" href="/admin/practice-areas">Practice Areas</a></li>
            <li><a class="dropdown-item" href="/admin/retention">Retention Settings</a></li>
            <li><a class="dropdown-item" href="/admin/import-export">Import / Export</a></li>
            <li><a class="dropdown-item" href="/admin/audit-log">Audit Log</a></li>
            <li><a class="dropdown-item" href="/admin/api-keys">API Keys</a></li>
          </ul>
        </li>
        </#if>
      </ul>
      <ul class="navbar-nav">
        <li class="nav-item"><span class="nav-link text-light-emphasis">Notifications: <span class="badge bg-warning text-dark">${unreadNotifications}</span></span></li>
        <li class="nav-item"><a class="nav-link" href="/profile">${user.fullName} (${user.role})</a></li>
        <li class="nav-item">
          <form method="post" action="/logout" class="d-inline">
            <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
            <button class="btn btn-outline-light btn-sm ms-2" type="submit">Log out</button>
          </form>
        </li>
      </ul>
    </div>
  </div>
</nav>
</#if>
<main class="container-fluid py-4 app-main">
  <#nested>
</main>
<footer class="text-center text-muted small py-3">Legal Case Management &mdash; demo application</footer>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
</#macro>

<#macro statusBadge status>
<#local cls = "secondary">
<#if status == "OPEN"><#local cls="success"></#if>
<#if status == "INTAKE"><#local cls="info"></#if>
<#if status == "CONFLICT_REVIEW"><#local cls="warning"></#if>
<#if status == "ON_HOLD"><#local cls="warning"></#if>
<#if status == "CLOSED"><#local cls="dark"></#if>
<#if status == "ARCHIVED"><#local cls="secondary"></#if>
<#if status == "COMPLETED"><#local cls="success"></#if>
<#if status == "BLOCKED"><#local cls="danger"></#if>
<#if status == "CANCELLED"><#local cls="secondary"></#if>
<#if status == "IN_PROGRESS"><#local cls="primary"></#if>
<#if status == "DRAFT"><#local cls="warning"></#if>
<#if status == "APPROVED"><#local cls="info"></#if>
<#if status == "SENT"><#local cls="success"></#if>
<span class="badge bg-${cls}"><i class="status-icon">&#9679;</i> ${status?replace('_',' ')}</span>
</#macro>
