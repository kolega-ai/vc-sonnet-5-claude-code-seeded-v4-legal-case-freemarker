<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="My Profile">
<h1 class="h3 mb-4">My Profile</h1>
<#if RequestParameters.updated??><div class="alert alert-success">Profile updated.</div></#if>
<#if RequestParameters.passwordChanged??><div class="alert alert-success">Password changed.</div></#if>
<div class="row g-4">
  <div class="col-md-6">
    <div class="card"><div class="card-body">
      <h2 class="h6">Profile</h2>
      <form method="post" action="/profile">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <div class="mb-3"><label class="form-label" for="email">Email</label><input class="form-control" id="email" value="${user.email}" disabled></div>
        <div class="mb-3"><label class="form-label" for="fullName">Full name</label><input class="form-control" id="fullName" name="fullName" value="${profileForm.fullName}" required></div>
        <div class="mb-3"><label class="form-label" for="role">Role</label><input class="form-control" id="role" value="${user.role}" disabled></div>
        <button class="btn btn-primary" type="submit">Save</button>
      </form>
    </div></div>
  </div>
  <div class="col-md-6">
    <div class="card"><div class="card-body">
      <h2 class="h6">Change Password</h2>
      <form method="post" action="/profile/password">
        <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
        <div class="mb-3"><label class="form-label" for="currentPassword">Current password</label><input class="form-control" id="currentPassword" name="currentPassword" type="password" required></div>
        <div class="mb-3"><label class="form-label" for="newPassword">New password</label><input class="form-control" id="newPassword" name="newPassword" type="password" minlength="8" required></div>
        <button class="btn btn-outline-primary" type="submit">Change Password</button>
      </form>
    </div></div>
  </div>
</div>
</@layout.page>
