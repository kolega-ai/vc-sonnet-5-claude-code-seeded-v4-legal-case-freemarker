<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><title>Client portal activation</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<div class="container" style="max-width:420px; margin-top:8vh;">
  <div class="card shadow-sm"><div class="card-body p-4">
    <h1 class="h5 mb-3">Activate your client portal</h1>
    <#if activated?? && activated>
      <div class="alert alert-success">Your portal access is active. <a href="/login">Sign in</a>.</div>
    <#else>
      <p class="small text-muted">Choose a passcode for ${contactAddress}.</p>
      <form method="post" action="/portal-invite">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <input type="hidden" name="e" value="${contactAddress}"/>
        <div class="mb-3">
          <label class="form-label" for="newPasscode">Passcode</label>
          <input class="form-control" id="newPasscode" name="newPasscode" type="password" minlength="8" required>
        </div>
        <button class="btn btn-primary w-100" type="submit">Activate portal access</button>
      </form>
    </#if>
  </div></div>
</div>
</body></html>
