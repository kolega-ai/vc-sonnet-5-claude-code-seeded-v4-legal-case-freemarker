<#ftl output_format="HTML">
<#import "/layout/main.ftlh" as layout>
<@layout.page title="New Client">
<h1 class="h3 mb-4">New Client</h1>
<div class="card"><div class="card-body">
<form method="post" action="/clients">
  <input type="hidden" name="${csrfToken.parameterName}" value="${csrfToken.token}"/>
  <div class="mb-3"><label class="form-label" for="name">Name</label><input class="form-control" id="name" name="name" required></div>
  <div class="mb-3"><label class="form-label" for="type">Type</label>
    <select class="form-select" id="type" name="type"><option value="PERSON">Person</option><option value="ORGANIZATION">Organization</option></select>
  </div>
  <div class="mb-3"><label class="form-label" for="nationalId">Tax / National ID</label><input class="form-control" id="nationalId" name="nationalId"></div>
  <div class="mb-3"><label class="form-label" for="email">Email</label><input class="form-control" id="email" name="email" type="email"></div>
  <div class="mb-3"><label class="form-label" for="phone">Phone</label><input class="form-control" id="phone" name="phone"></div>
  <div class="mb-3"><label class="form-label" for="address">Address</label><input class="form-control" id="address" name="address"></div>
  <div class="mb-3"><label class="form-label" for="conflictCheckNotes">Conflict Check Notes</label><textarea class="form-control" id="conflictCheckNotes" name="conflictCheckNotes" rows="2"></textarea></div>
  <button class="btn btn-primary" type="submit">Create Client</button>
</form>
</div></div>
</@layout.page>
