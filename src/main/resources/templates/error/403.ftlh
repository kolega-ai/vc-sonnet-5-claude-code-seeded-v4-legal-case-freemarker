<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><title>Access Denied</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<div class="container text-center" style="margin-top:12vh;">
  <h1 class="display-5">403 &mdash; Access Denied</h1>
  <p class="text-muted">${(message)!'You do not have permission to view this resource.'}</p>
  <a class="btn btn-primary" href="/dashboard">Back to Dashboard</a>
</div>
</body></html>
