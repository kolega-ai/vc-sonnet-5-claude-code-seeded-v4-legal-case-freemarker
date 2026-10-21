<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><title>Invalid Request</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<div class="container text-center" style="margin-top:12vh;">
  <h1 class="display-5">400 &mdash; Invalid Request</h1>
  <p class="text-muted">${(message)!'That action is not allowed in the current state.'}</p>
  <a class="btn btn-primary" href="/dashboard">Back to Dashboard</a>
</div>
</body></html>
