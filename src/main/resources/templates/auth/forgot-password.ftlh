<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><title>Forgot password</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<div class="container" style="max-width:420px; margin-top:8vh;">
  <div class="card shadow-sm"><div class="card-body p-4">
    <h1 class="h5 mb-3">Reset your password</h1>
    <#if noSuchAccount?? && noSuchAccount>
      <div class="alert alert-warning">No account is registered with that email address.</div>
    </#if>
    <#if submitted?? && submitted>
      <div class="alert alert-success">If that email exists in our system, a reset link has been sent (see the outbox in a real deployment; here it is simulated).</div>
    </#if>
    <form method="post" action="/forgot-password">
      <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
      <div class="mb-3">
        <label class="form-label" for="email">Email</label>
        <input class="form-control" id="email" name="email" type="email" required value="${(forgotPasswordForm.email)!''}">
      </div>
      <button class="btn btn-primary w-100" type="submit">Send reset link</button>
    </form>
    <div class="text-center mt-3"><a href="/login">Back to login</a></div>
  </div></div>
</div>
</body></html>
