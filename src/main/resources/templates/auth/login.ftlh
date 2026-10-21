<#ftl output_format="HTML">
<#import "/spring.ftl" as spring>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Sign in - Legal Case Management</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="/css/app.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container" style="max-width:420px; margin-top:8vh;">
  <div class="card shadow-sm">
    <div class="card-body p-4">
      <h1 class="h4 mb-3 text-center">Lex<span class="text-warning">Manage</span></h1>
      <p class="text-muted text-center small">Legal Case Management</p>
      <#if RequestParameters.error??>
        <div class="alert alert-danger" role="alert">Invalid email or password.</div>
      </#if>
      <#if RequestParameters.logout??>
        <div class="alert alert-success" role="alert">You have been logged out.</div>
      </#if>
      <form method="post" action="/login">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <div class="mb-3">
          <label class="form-label" for="username">Email</label>
          <input class="form-control" id="username" name="username" type="email" required autofocus>
        </div>
        <div class="mb-3">
          <label class="form-label" for="password">Password</label>
          <input class="form-control" id="password" name="password" type="password" required>
        </div>
        <button class="btn btn-primary w-100" type="submit">Sign in</button>
      </form>
      <div class="text-center mt-3">
        <a href="/forgot-password">Forgot your password?</a>
      </div>
      <hr>
      <p class="small text-muted mb-0">Demo accounts (password in README): admin@lawfirm.test, attorney@lawfirm.test, paralegal@lawfirm.test, client@lawfirm.test</p>
    </div>
  </div>
</div>
</body>
</html>
