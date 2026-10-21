<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><title>Reset password</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<div class="container" style="max-width:420px; margin-top:8vh;">
  <div class="card shadow-sm"><div class="card-body p-4">
    <h1 class="h5 mb-3">Choose a new password</h1>
    <#if success??>
      <div class="alert alert-success">Your password has been reset. <a href="/login">Sign in</a>.</div>
    <#elseif failed??>
      <div class="alert alert-danger">This reset link is invalid, already used, or has expired. <a href="/forgot-password">Request a new one</a>.</div>
    <#else>
      <form method="post" action="/reset-password">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <input type="hidden" name="token" value="${resetPasswordForm.token}"/>
        <div class="mb-3">
          <label class="form-label" for="newPassword">New password</label>
          <input class="form-control" id="newPassword" name="newPassword" type="password" minlength="8" required>
        </div>
        <button class="btn btn-primary w-100" type="submit">Reset password</button>
      </form>
    </#if>
  </div></div>
</div>
</body></html>
